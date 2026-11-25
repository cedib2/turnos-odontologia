package com.turnosodontologia.export;

import com.turnosodontologia.audit.AuditoriaCatalogo;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditEventCatalogTest {

    private static final Instant OCURRIDO = Instant.parse("2026-11-25T03:00:00Z");

    @Test
    void eventosInmutablesDeExportacion() {
        InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();
        ActorSimulado actor = ActorSimulado.recepcionista();
        Map<String, Object> metadata = Map.of("origen", "test");

        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.EXPORT_REQUESTED, "corr-exp",
                actor, "solicitud", metadata, OCURRIDO, "OK"));
        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.EXPORT_COMPLETED, "corr-exp",
                actor, "completado", metadata, OCURRIDO.plusSeconds(1), "OK"));

        List<AuditEvent> eventos = recorder.eventos();
        assertAll(
                () -> assertEquals(List.of("EXPORT_REQUESTED", "EXPORT_COMPLETED"),
                        eventos.stream().map(AuditEvent::tipo).toList()),
                () -> assertTrue(AuditoriaCatalogo.TIPOS.containsAll(ExportAuditEvents.TIPOS)),
                () -> assertDoesNotThrow(() -> new AuditoriaCatalogo().validar(
                        ExportAuditEvents.EXPORT_REQUESTED)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AuditoriaCatalogo().validar("EXPORT_COMPLETED_FAKE")),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> AuditoriaCatalogo.TIPOS.add("EVENTO_NUEVO")),
                () -> assertEquals("corr-exp", eventos.get(0).metadata().get("correlationId")),
                () -> assertEquals("recepcionista", eventos.get(0).metadata().get("actor")),
                () -> assertEquals("OK", eventos.get(1).metadata().get("resultado")),
                () -> assertEquals(OCURRIDO.toString(), eventos.get(0).metadata().get("occurredAt"))
        );
    }

    @Test
    void eventoDeFalloDeExportacion() {
        InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();

        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.EXPORT_FAILED, "corr-fallo",
                ActorSimulado.system(), "destino indisponible", Map.of("origen", "test"),
                OCURRIDO, "FALLIDO"));

        AuditEvent evento = recorder.eventos().get(0);
        assertAll(
                () -> assertEquals(1, recorder.eventos().size()),
                () -> assertEquals("EXPORT_FAILED", evento.tipo()),
                () -> assertEquals("destino indisponible", evento.metadata().get("motivo")),
                () -> assertEquals("FALLIDO", evento.metadata().get("resultado")),
                () -> assertEquals("corr-fallo", evento.correlationId())
        );
    }

    @Test
    void eventosDeRespaldoEjecutadoYFallido() {
        InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();

        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.BACKUP_EXECUTED, "corr-bk-ok",
                ActorSimulado.system(), "respaldo diario", Map.of("tenant", "t1"), OCURRIDO, "OK"));
        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.BACKUP_FAILED, "corr-bk-ko",
                ActorSimulado.system(), "destino indisponible", Map.of("tenant", "t1"),
                OCURRIDO.plusSeconds(1), "FALLIDO"));

        assertAll(
                () -> assertEquals(List.of("BACKUP_EXECUTED", "BACKUP_FAILED"),
                        recorder.eventos().stream().map(AuditEvent::tipo).toList()),
                () -> assertEquals("OK", recorder.eventos().get(0).metadata().get("resultado")),
                () -> assertEquals("FALLIDO", recorder.eventos().get(1).metadata().get("resultado")),
                () -> assertDoesNotThrow(() -> {
                    new AuditoriaCatalogo().validar(ExportAuditEvents.BACKUP_EXECUTED);
                    new AuditoriaCatalogo().validar(ExportAuditEvents.BACKUP_FAILED);
                })
        );
    }

    @Test
    void eventosDeRestoreVerificadoYEjecutado() {
        InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();

        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.RESTORE_DRY_RUN_VERIFIED,
                "corr-restore", ActorSimulado.system(), "cadena y conteos verificados",
                Map.of("entorno", "aislado"), OCURRIDO, "VERIFICADO"));
        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.RESTORE_EXECUTED,
                "corr-restore", ActorSimulado.system(), "restore en aislado",
                Map.of("entorno", "aislado"), OCURRIDO, "OK"));

        assertAll(
                () -> assertEquals(List.of("RESTORE_DRY_RUN_VERIFIED", "RESTORE_EXECUTED"),
                        recorder.eventos().stream().map(AuditEvent::tipo).toList()),
                () -> assertEquals(recorder.eventos().get(0).correlationId(),
                        recorder.eventos().get(1).correlationId()),
                () -> assertEquals(recorder.eventos().get(0).occurredAt(),
                        recorder.eventos().get(1).occurredAt()),
                () -> assertEquals("aislado", recorder.eventos().get(1).metadata().get("entorno"))
        );
    }

    @Test
    void correlacionDistintaEntreOperaciones() {
        InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();

        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.EXPORT_COMPLETED, "corr-export",
                ActorSimulado.recepcionista(), "completado", Map.of(), OCURRIDO, "OK"));
        recorder.publish(ExportAuditEvents.de(ExportAuditEvents.BACKUP_EXECUTED, "corr-backup",
                ActorSimulado.system(), "respaldo diario", Map.of(), OCURRIDO, "OK"));

        assertAll(
                () -> assertEquals(2, recorder.eventos().size()),
                () -> assertNotEquals(recorder.eventos().get(0).correlationId(),
                        recorder.eventos().get(1).correlationId()),
                () -> assertEquals("corr-export", recorder.eventos().get(0).metadata().get("correlationId")),
                () -> assertEquals("corr-backup", recorder.eventos().get(1).metadata().get("correlationId"))
        );
    }
}
