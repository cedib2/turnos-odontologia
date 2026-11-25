package com.turnosodontologia.application.cumplimiento;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Exportación completa de datos y copia por paciente (C-17, RN-SE-03, US-014).
 * Cada solicitud, resultado y descarga queda en el log append-only (C-05).
 */
@Service
public class ExportacionService {

    /**
     * Material de credenciales que nunca sale en una exportación (sí en el respaldo cifrado).
     * Al integrar C-10/C-15: toda tabla nueva que guarde secretos (tokens, firmas de webhook) se agrega acá.
     */
    static final Set<String> TABLAS_CREDENCIALES = Set.of(
            "auth_sesiones", "auth_tokens_refresh", "auth_jti_revocados",
            "public_token_jti", "public_idempotencia");
    static final Map<String, Set<String>> COLUMNAS_CREDENCIALES = Map.of("usuarios", Set.of("hash_password"));
    /**
     * Tablas incluidas en la copia de alcance PACIENTE: condición sobre el alias {@code t} con un único parámetro,
     * el id del paciente. Cada tabla se alcanza siguiendo sus claves hasta el paciente: por turno (agenda,
     * notificaciones C-10, lista de espera C-19), por ficha (historia clínica C-11 y firmas C-12) o por
     * presupuesto y seña (C-14/C-15/C-16). {@code lista_espera} entra solo por turno: {@code candidato_id} no
     * tiene FK y no está confirmado que sea un id de paciente. La vista vigente de la historia llega además por
     * {@link ProveedorHistoriaClinica} (C-11/C-13).
     */
    static final Map<String, String> FILTROS_PACIENTE = Map.ofEntries(
            Map.entry("pacientes", "t.id = ?"),
            Map.entry("turnos", "t.paciente_id = ?"),
            Map.entry("ocupaciones", porTurno("t.turno_id")),
            Map.entry("notificaciones", porTurno("t.turno_id")),
            Map.entry("notificaciones_outbox", "t.notificacion_id IN (SELECT n.id FROM notificaciones n "
                    + "JOIN turnos tu ON tu.id = n.turno_id WHERE tu.paciente_id = ?)"),
            Map.entry("notificacion_intentos", "t.notificacion_id IN (SELECT n.id FROM notificaciones n "
                    + "JOIN turnos tu ON tu.id = n.turno_id WHERE tu.paciente_id = ?)"),
            Map.entry("lista_espera", porTurno("t.turno_id")),
            Map.entry("fichas_clinicas", "t.paciente_id = ?"),
            Map.entry("anamnesis", porFicha("t.ficha_id")),
            Map.entry("odontograma_registros", porFicha("t.ficha_id")),
            Map.entry("evoluciones", porFicha("t.ficha_id")),
            Map.entry("consentimientos", porFicha("t.ficha_id")),
            Map.entry("evolucion_firmas", "t.evolucion_id IN (SELECT e.id FROM evoluciones e "
                    + "JOIN fichas_clinicas f ON f.id = e.ficha_id WHERE f.paciente_id = ?)"),
            Map.entry("consentimiento_firmas", "t.consentimiento_id IN (SELECT c.id FROM consentimientos c "
                    + "JOIN fichas_clinicas f ON f.id = c.ficha_id WHERE f.paciente_id = ?)"),
            Map.entry("historia_copias", "t.paciente_id = ?"),
            Map.entry("presupuestos", "t.paciente_id = ?"),
            Map.entry("items_presupuesto", "t.presupuesto_id IN (SELECT id FROM presupuestos WHERE paciente_id = ?)"),
            Map.entry("senias", "? IN (SELECT paciente_id FROM turnos WHERE id = t.turno_id "
                    + "UNION ALL SELECT paciente_id FROM presupuestos WHERE id = t.presupuesto_id)"),
            Map.entry("comprobantes", "? IN (SELECT paciente_id FROM turnos WHERE id = t.turno_id "
                    + "UNION ALL SELECT paciente_id FROM presupuestos WHERE id = t.presupuesto_id "
                    + "UNION ALL SELECT tu.paciente_id FROM senias s JOIN turnos tu ON tu.id = s.turno_id "
                    + "WHERE s.id = t.senia_id "
                    + "UNION ALL SELECT p.paciente_id FROM senias s JOIN presupuestos p ON p.id = s.presupuesto_id "
                    + "WHERE s.id = t.senia_id)"));

    /** Condición «la fila pertenece a un turno del paciente». */
    private static String porTurno(String columna) {
        return columna + " IN (SELECT id FROM turnos WHERE paciente_id = ?)";
    }

    /** Condición «la fila pertenece a la ficha clínica del paciente». */
    private static String porFicha(String columna) {
        return columna + " IN (SELECT id FROM fichas_clinicas WHERE paciente_id = ?)";
    }

    private final DataSource dataSource;
    private final RegistroOperaciones registro;
    private final CifradorArtefactos cifrador;
    private final AlmacenArtefactos almacen;
    private final ParametrosCumplimiento parametros;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher publisher;
    private final TransactionTemplate tx;
    private final ObjectProvider<ProveedorHistoriaClinica> historias;
    private final Clock clock;

    public ExportacionService(DataSource dataSource, RegistroOperaciones registro, CifradorArtefactos cifrador,
                              AlmacenArtefactos almacen, ParametrosCumplimiento parametros,
                              CurrentActorProvider actores, AuditEventPublisher publisher,
                              PlatformTransactionManager transactionManager,
                              ObjectProvider<ProveedorHistoriaClinica> historias,
                              ObjectProvider<Clock> clock) {
        this.dataSource = dataSource;
        this.registro = registro;
        this.cifrador = cifrador;
        this.almacen = almacen;
        this.parametros = parametros;
        this.actores = actores;
        this.publisher = publisher;
        this.tx = new TransactionTemplate(transactionManager);
        this.historias = historias;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    /** Registra la solicitud; el plazo de entrega se calcula solo si está configurado (TBD, IN-05). */
    public OperacionCumplimiento solicitar(String alcance, Long pacienteId, String motivo) {
        if (!OperacionCumplimiento.COMPLETA.equals(alcance) && !OperacionCumplimiento.PACIENTE.equals(alcance)) {
            throw new IllegalArgumentException("Alcance de exportación inválido: " + alcance);
        }
        if (OperacionCumplimiento.PACIENTE.equals(alcance) && pacienteId == null) {
            throw new IllegalArgumentException("La copia por paciente requiere pacienteId");
        }
        if (OperacionCumplimiento.COMPLETA.equals(alcance) && pacienteId != null) {
            throw new IllegalArgumentException("La exportación completa no admite pacienteId");
        }
        ActorSimulado actor = actores.current();
        Instant ahora = clock.instant();
        return tx.execute(estado -> {
            long id = registro.solicitar(OperacionCumplimiento.EXPORTACION, alcance, pacienteId,
                    Paquete.FORMATO, motivo, actor.nombre(), ahora, parametros.plazoDesde(ahora), null, null);
            OperacionCumplimiento operacion = registro.buscar(id).orElseThrow();
            CumplimientoAuditoria.publicar(publisher, actor, "exportacion.solicitada", operacion, motivo);
            return operacion;
        });
    }

    /** Genera, cifra y guarda la exportación. Ante cualquier fallo la operación queda FALLIDA y auditada. */
    public OperacionCumplimiento procesar(long id) {
        OperacionCumplimiento operacion = registro.buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Exportación inexistente: " + id));
        if (!OperacionCumplimiento.EXPORTACION.equals(operacion.tipo())
                || !OperacionCumplimiento.SOLICITADA.equals(operacion.estado())) {
            throw new IllegalStateException("La operación " + id + " no es una exportación pendiente");
        }
        ActorSimulado actor = actores.current();
        try {
            Paquete paquete = generar(operacion);
            byte[] plano = paquete.serializar();
            String checksum = Paquete.sha256(plano);
            String destino = almacen.guardar("exportacion-" + id + "-" + java.util.UUID.randomUUID() + ".bin", cifrador.cifrar(plano));
            Instant fin = clock.instant();
            return tx.execute(estado -> {
                registro.completar(id, destino, true, checksum, paquete.tablas().size(), paquete.totalFilas(),
                        fin, null);
                OperacionCumplimiento completada = registro.buscar(id).orElseThrow();
                CumplimientoAuditoria.publicar(publisher, actor, "exportacion.completada", completada,
                        operacion.motivo());
                return completada;
            });
        } catch (SQLException | RuntimeException e) {
            return tx.execute(estado -> {
                registro.fallar(id, mensaje(e), clock.instant());
                OperacionCumplimiento fallida = registro.buscar(id).orElseThrow();
                CumplimientoAuditoria.publicar(publisher, actor, "exportacion.fallida", fallida,
                        operacion.motivo());
                return fallida;
            });
        }
    }

    public OperacionCumplimiento exportar(String alcance, Long pacienteId, String motivo) {
        return procesar(solicitar(alcance, pacienteId, motivo).id());
    }

    /** Devuelve el contenido descifrado tras verificar su integridad; la descarga queda auditada. */
    public byte[] descargar(long id, String motivo) {
        OperacionCumplimiento operacion = registro.buscar(id)
                .filter(o -> OperacionCumplimiento.EXPORTACION.equals(o.tipo()) && o.completada())
                .orElseThrow(() -> new IllegalArgumentException("Exportación no disponible: " + id));
        byte[] plano = cifrador.descifrar(almacen.leer(operacion.destino()));
        if (!Paquete.sha256(plano).equals(operacion.checksumSha256())) {
            throw new IllegalStateException("La exportación " + id + " no coincide con su checksum registrado");
        }
        ActorSimulado actor = actores.current();
        tx.executeWithoutResult(estado ->
                CumplimientoAuditoria.publicar(publisher, actor, "exportacion.descargada", operacion, motivo));
        return plano;
    }

    public List<OperacionCumplimiento> vencidas(Instant ahora) {
        return registro.vencidas(ahora);
    }

    private Paquete generar(OperacionCumplimiento operacion) throws SQLException {
        boolean porPaciente = OperacionCumplimiento.PACIENTE.equals(operacion.alcance());
        Map<String, Object> anexos = new LinkedHashMap<>();
        anexos.put("formatoOficial", "(TBD, RN-SE-03)");
        anexos.put("credencialesExcluidas", new TreeSet<>(TABLAS_CREDENCIALES));
        anexos.put("columnasExcluidas", Map.of("usuarios", List.of("hash_password")));
        if (porPaciente) {
            ProveedorHistoriaClinica proveedor = historias.getIfAvailable();
            anexos.put("historiaClinica", proveedor == null
                    ? "(no disponible: sin proveedor de historia clínica)"
                    : proveedor.copiaHistoria(operacion.pacienteId()).map(Object.class::cast)
                    .orElse("(sin historia registrada)"));
        }
        VolcadoBaseDatos.Seleccion seleccion = porPaciente
                ? new VolcadoBaseDatos.Seleccion(TABLAS_CREDENCIALES, COLUMNAS_CREDENCIALES, FILTROS_PACIENTE,
                operacion.pacienteId(), true)
                : new VolcadoBaseDatos.Seleccion(TABLAS_CREDENCIALES, COLUMNAS_CREDENCIALES, Map.of(), null, false);
        try (Connection conexion = dataSource.getConnection()) {
            return VolcadoBaseDatos.volcar(conexion, OperacionCumplimiento.EXPORTACION, operacion.alcance(),
                    clock.instant().toString(), seleccion, anexos);
        }
    }

    static String mensaje(Exception e) {
        String mensaje = e.getMessage();
        return e.getClass().getSimpleName() + (mensaje == null ? "" : ": " + mensaje);
    }
}
