package com.turnosodontologia.export;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.audit.AuditoriaCanonicalizer;
import com.turnosodontologia.audit.AuditoriaIntegridadService;
import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(RealAuditPublisherConfig.class)
class RestoreIsolatedTest extends PostgresFlywayTest {

    private static final ActorSimulado ACTOR = ActorSimulado.system();
    private static final List<String> DOMINIOS = List.of(
            "identidad", "agenda", "clinico", "financial", "auditoria");

    private final AuditoriaCanonicalizer canonicalizer = new AuditoriaCanonicalizer();

    @Autowired
    private AuditEventPublisher publicadorReal;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void restoreVerificadoEnAislado() {
        String correlationId = UUID.randomUUID().toString();
        publicar(ExportAuditEvents.de(ExportAuditEvents.EXPORT_COMPLETED, correlationId,
                ACTOR, "completado", Map.of("origen", "test"), Instant.now(), "OK"));
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        ExportSnapshot snapshot = snapshot(false, List.of());

        new RestoreService(eventos, integridad(), conteos())
                .restaurar(new RestoreDestino("aislado-postgres", true), snapshot, correlationId, ACTOR);

        assertAll(
                () -> assertEquals(List.of("RESTORE_DRY_RUN_VERIFIED", "RESTORE_EXECUTED"),
                        eventos.eventos().stream().map(AuditEvent::tipo).toList()),
                () -> assertEquals(correlationId, eventos.eventos().get(0).correlationId()),
                () -> assertEquals("VERIFICADO",
                        eventos.eventos().get(0).metadata().get("resultado")),
                () -> assertEquals("OK", eventos.eventos().get(1).metadata().get("resultado"))
        );
    }

    @Test
    void cadenaInvalidaAbortaSinEscribir() {
        String correlationId = UUID.randomUUID().toString();
        publicar(ExportAuditEvents.de(ExportAuditEvents.EXPORT_COMPLETED, correlationId,
                ACTOR, "completado", Map.of("origen", "test"), Instant.now(), "OK"));
        jdbcTemplate.update("INSERT INTO auditoria_log "
                        + "(fecha, actor_nombre, modo, accion, entidad, motivo, metadata, "
                        + "correlation_id, hash_anterior, hash_actual) "
                        + "VALUES (now(), 'system', 'SYSTEM', 'EXPORT_COMPLETED', "
                        + "'export_backup_restore', 'cadena rota', '{}'::jsonb, ?, ?, ?)",
                UUID.randomUUID().toString(), "0".repeat(64), "1".repeat(64));
        int antes = totalAuditoria();
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();

        assertThrows(IllegalArgumentException.class, () -> new RestoreService(
                eventos, integridad(), conteos()).restaurar(
                new RestoreDestino("aislado-postgres", true), snapshot(false, List.of()),
                correlationId, ACTOR));

        assertAll(
                () -> assertTrue(eventos.eventos().isEmpty()),
                () -> assertEquals(antes, totalAuditoria())
        );
    }

    @Test
    void conteosDivergentesAbortanSinEscribir() {
        String correlationId = UUID.randomUUID().toString();
        publicar(ExportAuditEvents.de(ExportAuditEvents.EXPORT_COMPLETED, correlationId,
                ACTOR, "completado", Map.of("origen", "test"), Instant.now(), "OK"));
        int antes = totalAuditoria();
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        ExportSnapshot divergente = snapshot(true, List.of("clinical"));

        assertThrows(IllegalArgumentException.class, () -> new RestoreService(
                eventos, integridad(), conteos()).restaurar(
                new RestoreDestino("aislado-postgres", true), divergente, correlationId, ACTOR));

        assertAll(
                () -> assertTrue(divergente.partialExport()),
                () -> assertTrue(eventos.eventos().isEmpty()),
                () -> assertEquals(antes, totalAuditoria())
        );
    }

    @Test
    void destinoProduccionRechazado() {
        String correlationId = UUID.randomUUID().toString();
        publicar(ExportAuditEvents.de(ExportAuditEvents.EXPORT_COMPLETED, correlationId,
                ACTOR, "completado", Map.of("origen", "test"), Instant.now(), "OK"));
        int antes = totalAuditoria();
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();

        assertThrows(IllegalArgumentException.class, () -> new RestoreService(
                eventos, integridad(), conteos()).restaurar(
                new RestoreDestino("produccion", false), snapshot(false, List.of()),
                correlationId, ACTOR));

        assertAll(
                () -> assertTrue(eventos.eventos().isEmpty()),
                () -> assertEquals(antes, totalAuditoria())
        );
    }

    private ExportSnapshot snapshot(boolean parcial, List<String> noIntegrados) {
        Map<String, Integer> conteos = new java.util.LinkedHashMap<>();
        for (String dominio : DOMINIOS) {
            conteos.put(dominio, 0);
        }
        conteos.put("clinico", parcial ? 1 : 0);
        conteos.put("auditoria", totalAuditoria());
        return new ExportSnapshot(Instant.now(), "America/Argentina/Buenos_Aires",
                "tenant-1", conteos, DOMINIOS, parcial, noIntegrados,
                parcial ? ExportResultado.PARCIAL : ExportResultado.COMPLETA);
    }

    private RestoreCountPort conteos() {
        return dominio -> "auditoria".equals(dominio)
                ? jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auditoria_log", Integer.class)
                : 0;
    }

    private int totalAuditoria() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auditoria_log", Integer.class);
    }

    private void publicar(AuditEvent evento) {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                publicadorReal.publish(evento));
    }

    private AuditoriaIntegridadService integridad() {
        return new AuditoriaIntegridadService(jdbcTemplate, canonicalizer);
    }
}
