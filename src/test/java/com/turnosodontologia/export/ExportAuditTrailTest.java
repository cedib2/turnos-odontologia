package com.turnosodontologia.export;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.audit.AuditoriaCanonicalizer;
import com.turnosodontologia.audit.AuditoriaIntegridadService;
import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(RealAuditPublisherConfig.class)
class ExportAuditTrailTest extends PostgresFlywayTest {

    private static final ActorSimulado ACTOR = ActorSimulado.recepcionista();
    private static final Instant OCURRIDO = Instant.parse("2026-11-25T03:00:00Z");

    private final AuditoriaCanonicalizer canonicalizer = new AuditoriaCanonicalizer();

    @Autowired
    private AuditEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void exportacionConEventosRequestedYCompleted() {
        String correlationId = UUID.randomUUID().toString();

        registrar(correlationId, "EXPORTACION", "OK", "exportacion completada", List.of(
                evento(ExportAuditEvents.EXPORT_REQUESTED, correlationId, "solicitud", "OK"),
                evento(ExportAuditEvents.EXPORT_COMPLETED, correlationId, "completado", "OK")));

        List<Map<String, Object>> filas = filas(correlationId);
        assertAll(
                () -> assertEquals(List.of("EXPORT_REQUESTED", "EXPORT_COMPLETED"),
                        filas.stream().map(fila -> fila.get("accion").toString()).toList()),
                () -> assertEquals("recepcionista", filas.get(0).get("actor_nombre")),
                () -> assertNotNull(filas.get(0).get("fecha")),
                () -> assertEquals("OK", metadata(filas.get(1)).get("resultado")),
                () -> assertEquals(1, operaciones(correlationId)),
                () -> assertTrue(integridad().verificar().valida())
        );
    }

    @Test
    void exportacionFallidaConExportFailed() {
        String correlationId = UUID.randomUUID().toString();

        registrar(correlationId, "EXPORTACION", "FALLIDO", "destino indisponible", List.of(
                evento(ExportAuditEvents.EXPORT_FAILED, correlationId, "destino indisponible", "FALLIDO")));

        List<Map<String, Object>> filas = filas(correlationId);
        assertAll(
                () -> assertEquals(List.of("EXPORT_FAILED"),
                        filas.stream().map(fila -> fila.get("accion").toString()).toList()),
                () -> assertEquals("destino indisponible", metadata(filas.get(0)).get("motivo")),
                () -> assertEquals("FALLIDO", metadata(filas.get(0)).get("resultado")),
                () -> assertFalse(filas.stream().anyMatch(fila ->
                        "EXPORT_COMPLETED".equals(fila.get("accion").toString())))
        );
    }

    @Test
    void respaldoConEventosExecutadoYFallido() {
        String correcto = UUID.randomUUID().toString();
        String fallido = UUID.randomUUID().toString();

        registrar(correcto, "RESPALDO", "OK", "respaldo diario", List.of(
                evento(ExportAuditEvents.BACKUP_EXECUTED, correcto, "respaldo diario", "OK")));
        registrar(fallido, "RESPALDO", "FALLIDO", "destino indisponible", List.of(
                evento(ExportAuditEvents.BACKUP_FAILED, fallido, "destino indisponible", "FALLIDO")));

        assertAll(
                () -> assertEquals(List.of("BACKUP_EXECUTED"),
                        filas(correcto).stream().map(fila -> fila.get("accion").toString()).toList()),
                () -> assertEquals(List.of("BACKUP_FAILED"),
                        filas(fallido).stream().map(fila -> fila.get("accion").toString()).toList()),
                () -> assertEquals("OK", metadata(filas(correcto).get(0)).get("resultado")),
                () -> assertEquals("FALLIDO", metadata(filas(fallido).get(0)).get("resultado")),
                () -> assertEquals(2, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM export_backup_restore_operacion "
                                + "WHERE operacion = 'RESPALDO'", Integer.class))
        );
    }

    @Test
    void restoreConDryRunYExecuted() {
        String correlationId = UUID.randomUUID().toString();

        registrar(correlationId, "RESTORE", "OK", "restore en aislado", List.of(
                evento(ExportAuditEvents.RESTORE_DRY_RUN_VERIFIED, correlationId,
                        "cadena y conteos verificados", "VERIFICADO"),
                evento(ExportAuditEvents.RESTORE_EXECUTED, correlationId,
                        "restore en aislado", "OK")));

        List<Map<String, Object>> filas = filas(correlationId);
        assertAll(
                () -> assertEquals(List.of("RESTORE_DRY_RUN_VERIFIED", "RESTORE_EXECUTED"),
                        filas.stream().map(fila -> fila.get("accion").toString()).toList()),
                () -> assertEquals(correlationId, filas.get(0).get("correlation_id")),
                () -> assertEquals(correlationId, filas.get(1).get("correlation_id")),
                () -> assertEquals("VERIFICADO", metadata(filas.get(0)).get("resultado")),
                () -> assertTrue(integridad().verificar().valida())
        );
    }

    @Test
    void correlacionesDistintasEntreOperaciones() {
        String exportacion = UUID.randomUUID().toString();
        String respaldo = UUID.randomUUID().toString();
        String restore = UUID.randomUUID().toString();

        registrar(exportacion, "EXPORTACION", "OK", "completado", List.of(
                evento(ExportAuditEvents.EXPORT_COMPLETED, exportacion, "completado", "OK")));
        registrar(respaldo, "RESPALDO", "OK", "diario", List.of(
                evento(ExportAuditEvents.BACKUP_EXECUTED, respaldo, "diario", "OK")));
        registrar(restore, "RESTORE", "OK", "aislado", List.of(
                evento(ExportAuditEvents.RESTORE_EXECUTED, restore, "aislado", "OK")));

        List<String> correlaciones = jdbcTemplate.queryForList(
                "SELECT DISTINCT correlation_id FROM auditoria_log ORDER BY correlation_id", String.class);
        assertAll(
                () -> assertEquals(3, correlaciones.size()),
                () -> assertTrue(correlaciones.containsAll(List.of(exportacion, respaldo, restore))),
                () -> assertEquals(3, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM export_backup_restore_operacion", Integer.class)),
                () -> assertEquals(1, filas(exportacion).size()),
                () -> assertEquals(1, filas(respaldo).size()),
                () -> assertEquals(1, filas(restore).size())
        );
    }

    private AuditEvent evento(String tipo, String correlationId, String motivo, String resultado) {
        return ExportAuditEvents.de(tipo, correlationId, ACTOR, motivo,
                Map.of("origen", "test"), OCURRIDO, resultado);
    }

    private void registrar(String correlationId, String operacion, String resultado, String motivo,
                           List<AuditEvent> eventos) {
        ExportOperationRegistry registry = new ExportOperationRegistry(jdbcTemplate);
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            for (AuditEvent evento : eventos) {
                publisher.publish(evento);
            }
            registry.registrar(new OperacionRegistrada(correlationId, operacion, resultado,
                    motivo, Map.of("origen", "test"), "tenant-1", ACTOR.nombre()));
        });
    }

    private int operaciones(String correlationId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM export_backup_restore_operacion "
                + "WHERE correlation_id = ?", Integer.class, correlationId);
    }

    private Map<?, ?> metadata(Map<String, Object> fila) {
        return canonicalizer.leerMetadata(fila.get("metadata").toString());
    }

    private List<Map<String, Object>> filas(String correlationId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM auditoria_log WHERE correlation_id = ? ORDER BY id",
                correlationId);
    }

    private AuditoriaIntegridadService integridad() {
        return new AuditoriaIntegridadService(jdbcTemplate, canonicalizer);
    }
}
