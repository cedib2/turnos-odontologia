package com.turnosodontologia.application.cumplimiento;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.audit.AuditoriaIntegridadService;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** Arma, descarga y publica el dossier de cumplimiento con evidencia verificable (C-18). */
@Service
public class DossierService {

    static final String FORMULA = "100 × ítems obligatorios presentes y verificables / ítems obligatorios exigidos";
    static final String METODOLOGIA = "Ítems sembrados en V031 (RN-SE-04, C-01 D). Los AUTOMATICOS se evalúan "
            + "contra el sistema al generar el dossier; los MANUALES exigen referencia, verificador y fecha. "
            + "Un subprocesador sin cláusulas AAIP 198/2023 se excluye y deja pendiente el ítem.";

    private final JdbcTemplate jdbc;
    private final RegistroOperaciones operaciones;
    private final ParametrosCumplimiento parametros;
    private final CifradorArtefactos cifrador;
    private final AlmacenArtefactos almacen;
    private final ObjectProvider<AuditoriaIntegridadService> integridad;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher publisher;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DossierService(JdbcTemplate jdbc, RegistroOperaciones operaciones, ParametrosCumplimiento parametros,
                          CifradorArtefactos cifrador, AlmacenArtefactos almacen,
                          ObjectProvider<AuditoriaIntegridadService> integridad, CurrentActorProvider actores,
                          AuditEventPublisher publisher, PlatformTransactionManager transactionManager,
                          ObjectProvider<Clock> clock) {
        this.jdbc = jdbc;
        this.operaciones = operaciones;
        this.parametros = parametros;
        this.cifrador = cifrador;
        this.almacen = almacen;
        this.integridad = integridad;
        this.actores = actores;
        this.publisher = publisher;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    /** Arma el dossier sin registrar acceso (uso interno y métricas). */
    public Dossier generar() {
        List<Dossier.Subprocesador> subprocesadores = subprocesadoresActivos();
        List<Dossier.Subprocesador> incluidos = subprocesadores.stream()
                .filter(Dossier.Subprocesador::conClausulas).toList();
        List<Dossier.Subprocesador> excluidos = subprocesadores.stream()
                .filter(s -> !s.conClausulas()).toList();
        Map<String, List<String>> matriz = matrizRoles();

        List<Dossier.Item> items = new ArrayList<>();
        for (Map<String, Object> fila : jdbc.queryForList("SELECT * FROM dossier_items ORDER BY orden")) {
            Dossier.Item base = new Dossier.Item((String) fila.get("codigo"), (String) fila.get("descripcion"),
                    (String) fila.get("categoria"), (String) fila.get("fuente"),
                    (String) fila.get("base_normativa"), (Boolean) fila.get("obligatorio"),
                    (String) fila.get("estado"), (String) fila.get("evidencia_referencia"),
                    (String) fila.get("verificado_por"), instante(fila.get("verificado_at")));
            items.add("AUTOMATICA".equals(base.fuente())
                    ? evaluar(base, matriz, incluidos, excluidos)
                    : base);
        }
        List<String> pendientes = items.stream()
                .filter(i -> i.obligatorio() && !i.presente())
                .map(Dossier.Item::codigo)
                .toList();
        int exigidos = (int) items.stream().filter(Dossier.Item::obligatorio).count();
        int presentes = exigidos - pendientes.size();
        BigDecimal porcentaje = exigidos == 0
                ? null
                : BigDecimal.valueOf(presentes * 100L).divide(BigDecimal.valueOf(exigidos), 2, RoundingMode.HALF_UP);
        Dossier.Completitud completitud = new Dossier.Completitud(presentes, exigidos, porcentaje, FORMULA,
                METODOLOGIA, "(TBD) — baseline del piloto (C-01 E)");
        return new Dossier(clock.instant(), items, incluidos, excluidos, pendientes, completitud, matriz,
                parametrosOperativos(), pendientes.isEmpty());
    }

    /** Descarga del dossier (aun incompleto, con pendientes explícitos); queda auditada. */
    public Dossier descargar(String motivo) {
        Dossier dossier = generar();
        auditar("dossier.descargado", null, motivo, resumen(dossier));
        return dossier;
    }

    /** Publica el dossier solo si no quedan pendientes obligatorios; el intento bloqueado también se audita. */
    public Dossier publicar(String motivo) {
        Dossier dossier = generar();
        if (!dossier.publicable()) {
            auditar("dossier.publicacion_bloqueada", null, motivo, resumen(dossier));
            throw new DossierIncompletoException(dossier.pendientes());
        }
        auditar("dossier.publicado", null, motivo, resumen(dossier));
        return dossier;
    }

    /** Registra evidencia de un ítem MANUAL (referencia documental, verificador y fecha). */
    public Dossier.Item registrarEvidencia(String codigo, String referencia, String motivo) {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("La evidencia requiere una referencia verificable");
        }
        ActorSimulado actor = actores.current();
        return tx.execute(estado -> {
            int actualizados = jdbc.update("""
                    UPDATE dossier_items
                    SET estado = 'PRESENTE', evidencia_referencia = ?, verificado_por = ?, verificado_at = ?
                    WHERE codigo = ? AND fuente = 'MANUAL'
                    """, referencia.trim(), actor.nombre(), Timestamp.from(clock.instant()), codigo);
            if (actualizados != 1) {
                throw new IllegalArgumentException("Ítem manual inexistente: " + codigo);
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("codigo", codigo);
            metadata.put("referencia", referencia.trim());
            publisher.publish(AuditEvent.de("dossier.evidencia_registrada", "dossier_item", null, actor,
                    motivo == null ? "" : motivo, metadata, UUID.randomUUID().toString()));
            return generar().items().stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
        });
    }

    /**
     * Registra un subprocesador. Sin referencia a cláusulas AAIP 198/2023 queda registrado pero excluido
     * del dossier, y el ítem de subprocesadores sigue pendiente.
     */
    public Dossier.Subprocesador registrarSubprocesador(String nombre, String servicio, String ubicacionDatos,
                                                        String clausulasAaipReferencia, String dpaReferencia) {
        if (nombre == null || nombre.isBlank() || servicio == null || servicio.isBlank()) {
            throw new IllegalArgumentException("Nombre y servicio del subprocesador son obligatorios");
        }
        ActorSimulado actor = actores.current();
        return tx.execute(estado -> {
            Long id = jdbc.queryForObject("""
                    INSERT INTO subprocesadores
                        (nombre, servicio, ubicacion_datos, clausulas_aaip_referencia, dpa_referencia, created_by)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, nombre.trim(), servicio.trim(), vacioANull(ubicacionDatos),
                    vacioANull(clausulasAaipReferencia), vacioANull(dpaReferencia), actor.nombre());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("nombre", nombre.trim());
            metadata.put("conClausulas", clausulasAaipReferencia != null && !clausulasAaipReferencia.isBlank());
            publisher.publish(AuditEvent.de("dossier.subprocesador_registrado", "subprocesador", id, actor,
                    "", metadata, UUID.randomUUID().toString()));
            return new Dossier.Subprocesador(id, nombre.trim(), servicio.trim(), vacioANull(ubicacionDatos),
                    vacioANull(clausulasAaipReferencia), vacioANull(dpaReferencia));
        });
    }

    private Dossier.Item evaluar(Dossier.Item item, Map<String, List<String>> matriz,
                                 List<Dossier.Subprocesador> incluidos, List<Dossier.Subprocesador> excluidos) {
        Optional<String> evidencia = switch (item.codigo()) {
            case "ROLES_RBAC" -> matriz.isEmpty() || matriz.values().stream().anyMatch(List::isEmpty)
                    ? Optional.empty()
                    : Optional.of(matriz.size() + " roles con permisos asignados");
            case "LOG_AUDITORIA_INTEGRO" -> evidenciaAuditoria();
            case "EXPORTACION_COMPLETA" -> operaciones
                    .ultimaCompletada(OperacionCumplimiento.EXPORTACION, OperacionCumplimiento.COMPLETA)
                    .map(o -> "exportación #" + o.id() + " del " + o.completadoAt() + " sha256=" + o.checksumSha256());
            case "RESPALDO_RESTORE_VERIFICADO" -> operaciones
                    .ultimaCompletada(OperacionCumplimiento.RESTORE, OperacionCumplimiento.COMPLETA)
                    .map(o -> "restore #" + o.id() + " del respaldo #" + o.respaldoOrigenId()
                            + " verificado el " + o.completadoAt() + " (" + o.filas() + " filas)");
            case "CLAUSULAS_SUBPROCESADORES" -> incluidos.isEmpty() || !excluidos.isEmpty()
                    ? Optional.empty()
                    : Optional.of(incluidos.size() + " subprocesadores con cláusulas publicadas");
            default -> Optional.empty();
        };
        return new Dossier.Item(item.codigo(), item.descripcion(), item.categoria(), item.fuente(),
                item.baseNormativa(), item.obligatorio(), evidencia.isPresent() ? "PRESENTE" : "PENDIENTE",
                evidencia.orElse(null), evidencia.isPresent() ? "sistema" : null,
                evidencia.isPresent() ? clock.instant() : null);
    }

    private Optional<String> evidenciaAuditoria() {
        AuditoriaIntegridadService servicio = integridad.getIfAvailable();
        if (servicio == null) {
            return Optional.empty();
        }
        AuditoriaIntegridadService.Resultado resultado = servicio.verificar();
        return resultado.valida() && resultado.filasVerificadas() > 0
                ? Optional.of("cadena de hash válida sobre " + resultado.filasVerificadas() + " registros")
                : Optional.empty();
    }

    private Map<String, List<String>> matrizRoles() {
        Map<String, List<String>> matriz = new TreeMap<>();
        jdbc.query("""
                SELECT r.codigo, p.recurso || ':' || p.accion || ':' || p.ambito AS permiso
                FROM roles r
                LEFT JOIN rol_permiso rp ON rp.rol_id = r.id
                LEFT JOIN permisos p ON p.id = rp.permiso_id
                WHERE r.activo
                ORDER BY r.codigo, permiso
                """, rs -> {
            List<String> permisos = matriz.computeIfAbsent(rs.getString(1), k -> new ArrayList<>());
            String permiso = rs.getString(2);
            if (permiso != null) {
                permisos.add(permiso);
            }
        });
        return matriz;
    }

    private List<Dossier.Subprocesador> subprocesadoresActivos() {
        return jdbc.query("SELECT * FROM subprocesadores WHERE activo ORDER BY nombre", (rs, n) ->
                new Dossier.Subprocesador(rs.getLong("id"), rs.getString("nombre"), rs.getString("servicio"),
                        rs.getString("ubicacion_datos"), rs.getString("clausulas_aaip_referencia"),
                        rs.getString("dpa_referencia")));
    }

    private Map<String, String> parametrosOperativos() {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("plazoCopiaHoras", parametros.plazoCopiaHoras().map(String::valueOf).orElse("(TBD, IN-05)"));
        valores.put("retencionAnios", parametros.retencionAnios().map(String::valueOf).orElse("(TBD, IN-06)"));
        valores.put("cifradoEnReposo", cifrador.configurado() ? "AES-256-GCM" : "(pendiente: sin clave)");
        valores.put("destinoRespaldo", almacen.configurado() ? "configurado" : "(TBD)");
        valores.put("formatoExportacion", Paquete.FORMATO + " (formato oficial TBD)");
        return valores;
    }

    private Map<String, Object> resumen(Dossier dossier) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("presentes", dossier.completitud().presentes());
        metadata.put("exigidos", dossier.completitud().exigidos());
        metadata.put("pendientes", dossier.pendientes());
        metadata.put("publicable", dossier.publicable());
        return metadata;
    }

    private void auditar(String tipo, Long entidadId, String motivo, Map<String, Object> metadata) {
        ActorSimulado actor = actores.current();
        tx.executeWithoutResult(estado -> publisher.publish(AuditEvent.de(tipo, "dossier", entidadId, actor,
                motivo == null ? "" : motivo, metadata, UUID.randomUUID().toString())));
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static Instant instante(Object valor) {
        if (valor instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (valor instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        return null;
    }
}
