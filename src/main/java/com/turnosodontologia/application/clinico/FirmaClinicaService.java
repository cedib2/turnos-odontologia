package com.turnosodontologia.application.clinico;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Firma de evoluciones con matrícula (RN-CL-03) y de consentimientos con firma electrónica
 * (Ley 25.506, proveedor {@code (TBD)}), con un evento de auditoría por cada firma (RN-SE-02).
 *
 * <ul>
 *   <li>Sin matrícula, con profesional inactivo o distinto del autor de la evolución → no se
 *       firma y se audita {@code clinico.evolucion.firma_rechazada}.</li>
 *   <li>Solo se firma la versión vigente; una evolución firmada no se modifica: se registra una
 *       versión nueva con {@link HistoriaClinicaService#registrarEvolucion}, que nace sin firmar.</li>
 *   <li>La firma guarda el SHA-256 del contenido firmado para verificar integridad.</li>
 *   <li>Proveedor de firma no disponible → el consentimiento queda sin firmar y se audita
 *       {@code clinico.consentimiento.firma_fallida}; la contingencia es {@code (TBD)}.</li>
 * </ul>
 */
@Service
@Transactional(noRollbackFor = {FirmaRechazadaException.class, AccesoClinicoDenegadoException.class})
public class FirmaClinicaService {

    private final JdbcTemplate jdbc;
    private final CurrentActorProvider actores;
    private final AutorizacionClinica autorizacion;
    private final FirmaElectronicaGateway firmaElectronica;
    private final AuditEventPublisher auditoria;

    public FirmaClinicaService(JdbcTemplate jdbc, CurrentActorProvider actores, AutorizacionClinica autorizacion,
                               FirmaElectronicaGateway firmaElectronica, AuditEventPublisher auditoria) {
        this.jdbc = jdbc;
        this.actores = actores;
        this.autorizacion = autorizacion;
        this.firmaElectronica = firmaElectronica;
        this.auditoria = auditoria;
    }

    public long firmarEvolucion(long evolucionId, long profesionalId) {
        ActorSimulado actor = actores.current();
        Map<String, Object> evolucion = jdbc.queryForList("SELECT id, ficha_id, profesional_id, version, texto "
                + "FROM evoluciones WHERE id = ?", evolucionId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evolución inexistente: " + evolucionId));
        long fichaId = numero(evolucion.get("ficha_id"));
        autorizacion.exigir(actor, HistoriaClinicaService.CREAR, fichaId);
        jdbc.queryForList("SELECT id FROM fichas_clinicas WHERE id = ? FOR UPDATE", Long.class, fichaId);

        if (numero(evolucion.get("profesional_id")) != profesionalId) {
            rechazar(fichaId, evolucionId, actor, "profesional-distinto-del-autor");
        }
        Map<String, Object> profesional = jdbc.queryForList(
                "SELECT matricula, activo FROM profesionales WHERE id = ?", profesionalId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profesional inexistente: " + profesionalId));
        String matricula = (String) profesional.get("matricula");
        if (matricula == null || matricula.isBlank()) {
            rechazar(fichaId, evolucionId, actor, "matricula-faltante");
        }
        if (!Boolean.TRUE.equals(profesional.get("activo"))) {
            rechazar(fichaId, evolucionId, actor, "profesional-inactivo");
        }
        if (contar("SELECT COUNT(*) FROM evolucion_firmas WHERE evolucion_id = ?", evolucionId) > 0) {
            rechazar(fichaId, evolucionId, actor, "evolucion-ya-firmada");
        }
        if (contar("SELECT COUNT(*) FROM evoluciones WHERE reemplaza_id = ?", evolucionId) > 0) {
            rechazar(fichaId, evolucionId, actor, "evolucion-reemplazada");
        }

        String hash = hashEvolucion(evolucion);
        Long id = jdbc.queryForObject("INSERT INTO evolucion_firmas (evolucion_id, profesional_id, matricula, "
                        + "hash_contenido, firmante_id, firmante_nombre, firmante_modo) VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "RETURNING id", Long.class,
                evolucionId, profesionalId, matricula.trim(), hash, actor.usuarioId(), actor.nombre(), actor.modo());
        cerrarCambio(fichaId, "clinico.evolucion.firmada", actor,
                Map.of("evolucionId", evolucionId, "matricula", matricula.trim(), "version", evolucion.get("version")));
        return id;
    }

    public long firmarConsentimiento(long consentimientoId) {
        ActorSimulado actor = actores.current();
        Map<String, Object> consentimiento = jdbc.queryForList("SELECT c.id, c.ficha_id, c.tipo, c.version, c.texto, "
                + "f.paciente_id FROM consentimientos c JOIN fichas_clinicas f ON f.id = c.ficha_id WHERE c.id = ?",
                consentimientoId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Consentimiento inexistente: " + consentimientoId));
        long fichaId = numero(consentimiento.get("ficha_id"));
        autorizacion.exigir(actor, HistoriaClinicaService.CREAR, fichaId);
        jdbc.queryForList("SELECT id FROM fichas_clinicas WHERE id = ? FOR UPDATE", Long.class, fichaId);
        if (contar("SELECT COUNT(*) FROM consentimiento_firmas WHERE consentimiento_id = ?", consentimientoId) > 0) {
            auditar("clinico.consentimiento.firma_fallida", fichaId, actor, "consentimiento-ya-firmado",
                    Map.of("consentimientoId", consentimientoId));
            throw new FirmaRechazadaException("consentimiento-ya-firmado");
        }

        String hash = sha256(consentimiento.get("id") + "|" + consentimiento.get("ficha_id") + "|"
                + consentimiento.get("tipo") + "|" + consentimiento.get("version") + "|" + consentimiento.get("texto"));
        FirmaElectronicaGateway.ResultadoFirma resultado = firmaElectronica.firmar(
                new FirmaElectronicaGateway.SolicitudFirma(consentimientoId,
                        numero(consentimiento.get("paciente_id")), hash));
        if (!resultado.firmado()) {
            auditar("clinico.consentimiento.firma_fallida", fichaId, actor, resultado.error(),
                    Map.of("consentimientoId", consentimientoId));
            throw new FirmaRechazadaException("Firma electrónica no disponible: " + resultado.error());
        }
        Long id = jdbc.queryForObject("INSERT INTO consentimiento_firmas (consentimiento_id, proveedor, "
                        + "referencia_proveedor, hash_documento, registrado_por_id, registrado_por_nombre, "
                        + "registrado_por_modo) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id", Long.class,
                consentimientoId, resultado.proveedor(), resultado.referencia(), hash, actor.usuarioId(),
                actor.nombre(), actor.modo());
        cerrarCambio(fichaId, "clinico.consentimiento.firmado", actor,
                Map.of("consentimientoId", consentimientoId, "proveedor", resultado.proveedor()));
        return id;
    }

    /** Verifica que el contenido firmado de la evolución no haya cambiado desde la firma. */
    @Transactional(readOnly = true)
    public boolean evolucionIntegra(long evolucionId) {
        List<String> hashes = jdbc.queryForList(
                "SELECT hash_contenido FROM evolucion_firmas WHERE evolucion_id = ?", String.class, evolucionId);
        if (hashes.isEmpty()) {
            return false;
        }
        Map<String, Object> evolucion = jdbc.queryForMap("SELECT id, ficha_id, profesional_id, version, texto "
                + "FROM evoluciones WHERE id = ?", evolucionId);
        return hashes.get(0).equals(hashEvolucion(evolucion));
    }

    @Transactional(readOnly = true)
    public boolean evolucionFirmada(long evolucionId) {
        return contar("SELECT COUNT(*) FROM evolucion_firmas WHERE evolucion_id = ?", evolucionId) > 0;
    }

    private void rechazar(long fichaId, long evolucionId, ActorSimulado actor, String motivo) {
        auditar("clinico.evolucion.firma_rechazada", fichaId, actor, motivo, Map.of("evolucionId", evolucionId));
        throw new FirmaRechazadaException(motivo);
    }

    private void cerrarCambio(long fichaId, String tipo, ActorSimulado actor, Map<String, Object> metadata) {
        Integer version = jdbc.queryForObject("UPDATE fichas_clinicas SET historia_version = historia_version + 1, "
                + "updated_at = now() WHERE id = ? RETURNING historia_version", Integer.class, fichaId);
        Map<String, Object> completa = new java.util.HashMap<>(metadata);
        completa.put("historiaVersion", version);
        auditar(tipo, fichaId, actor, "", completa);
    }

    private void auditar(String tipo, long fichaId, ActorSimulado actor, String motivo, Map<String, Object> metadata) {
        auditoria.publish(AuditEvent.de(tipo, "ficha", fichaId, actor, motivo == null ? "" : motivo, metadata,
                UUID.randomUUID().toString()));
    }

    private String hashEvolucion(Map<String, Object> evolucion) {
        return sha256(evolucion.get("id") + "|" + evolucion.get("ficha_id") + "|" + evolucion.get("profesional_id")
                + "|" + evolucion.get("version") + "|" + evolucion.get("texto"));
    }

    private int contar(String sql, Object... args) {
        Integer valor = jdbc.queryForObject(sql, Integer.class, args);
        return valor == null ? 0 : valor;
    }

    private static long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
