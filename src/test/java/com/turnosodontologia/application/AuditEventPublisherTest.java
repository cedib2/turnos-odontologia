package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditEventPublisherTest {

    private final InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();
    private final AuditEventPublisher publisher = recorder;

    @Test
    void eventoDeCreacionContieneTodosLosCamposObligatoriosYModoSimulado() {
        ActorSimulado actor = ActorSimulado.recepcionista();

        publisher.publish(AuditEvent.creacionTurno(42L, actor, "fixture-motivo",
                Map.of("origen", "fixture-origen")));

        assertEquals(1, recorder.eventos().size());
        AuditEvent evento = recorder.eventos().get(0);
        assertAll(
                () -> assertNotNull(evento.tipo()),
                () -> assertEquals("turno", evento.entidad()),
                () -> assertEquals(42L, evento.entidadId()),
                () -> assertEquals("recepcionista", evento.actor().nombre()),
                () -> assertEquals("SIMULADO", evento.actor().modo()),
                () -> assertNotNull(evento.occurredAt()),
                () -> assertEquals("fixture-motivo", evento.motivo()),
                () -> assertEquals("fixture-origen", evento.metadata().get("origen")),
                () -> assertFalse(evento.correlationId().isBlank())
        );
    }

    @Test
    void eventosDeUnaOperacionCompartenCorrelacionYOperacionesDistintasNo() {
        String operacion = UUID.randomUUID().toString();
        String otraOperacion = UUID.randomUUID().toString();
        ActorSimulado actor = ActorSimulado.recepcionista();

        publisher.publish(AuditEvent.de("turno.creado", "turno", 1L, actor,
                "fixture", Map.of(), operacion));
        publisher.publish(AuditEvent.de("ocupacion.creada", "ocupacion", 2L, actor,
                "fixture", Map.of(), operacion));
        publisher.publish(AuditEvent.de("turno.creado", "turno", 3L, actor,
                "fixture", Map.of(), otraOperacion));

        assertEquals(3, recorder.eventos().size());
        assertEquals(operacion, recorder.eventos().get(0).correlationId());
        assertEquals(operacion, recorder.eventos().get(1).correlationId());
        assertNotEquals(operacion, recorder.eventos().get(2).correlationId());
    }

    @Test
    void noPersisteAuditoriaNiExponeApiDeConsulta() throws IOException {
        List<String> prohibidos = List.of("CREATE TABLE AUDIT", "/auditoria", "/audit");
        List<String> hallazgos = new java.util.ArrayList<>();
        List<Path> raices = List.of(Path.of("src", "main", "java"),
                Path.of("src", "main", "resources"));

        for (Path raiz : raices) {
            try (var paths = Files.walk(raiz)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String contenido = Files.readString(path).toUpperCase(java.util.Locale.ROOT);
                    for (String prohibido : prohibidos) {
                        if (contenido.contains(prohibido)) {
                            hallazgos.add(path + " -> " + prohibido);
                        }
                    }
                }
            }
        }

        assertEquals(List.of(), hallazgos);
    }
}
