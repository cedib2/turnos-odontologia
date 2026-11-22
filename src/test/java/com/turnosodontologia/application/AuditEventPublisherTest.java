package com.turnosodontologia.application;

import com.turnosodontologia.audit.AuditoriaCatalogo;
import com.turnosodontologia.audit.JdbcAuditEventPublisher;
import com.turnosodontologia.auth.AuthenticatedActor;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AuditEventPublisherTest {

    private final InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();
    private final AuditEventPublisher publisher = recorder;

    @Test
    void actorFijoDeDevTest() {
        ActorSimulado actor = ActorSimulado.recepcionista();
        publisher.publish(AuditEvent.creacionTurno(42L, actor, "fixture-motivo",
                Map.of("origen", "fixture-origen")));

        assertEquals(1, recorder.eventos().size());
        AuditEvent evento = recorder.eventos().get(0);
        assertAll(
                () -> assertNotNull(evento.tipo()),
                () -> assertEquals("turno.creado", evento.tipo()),
                () -> assertEquals("turno", evento.entidad()),
                () -> assertEquals(42L, evento.entidadId()),
                () -> assertEquals("recepcionista", evento.actor().nombre()),
                () -> assertEquals("SIMULADO", evento.actor().modo()),
                () -> assertEquals("RECEPCIONISTA", evento.actor().rol()),
                () -> assertNotNull(evento.occurredAt()),
                () -> assertEquals("fixture-motivo", evento.motivo()),
                () -> assertEquals("fixture-origen", evento.metadata().get("origen")),
                () -> assertFalse(evento.correlationId().isBlank())
        );
    }

    @Test
    void sinIdentidadVerificada() {
        publisher.publish(AuditEvent.de("turno.creado", "turno", 43L,
                ActorSimulado.paciente(99L, "paciente"), "fixture", Map.of(), "corr-paciente"));

        ActorSimulado actor = recorder.eventos().get(0).actor();
        assertAll(
                () -> assertEquals("PACIENTE", actor.modo()),
                () -> assertNull(actor.jti()),
                () -> assertEquals("PACIENTE", actor.rol())
        );
    }

    @Test
    void actorRealDeC04() {
        AuthenticatedActor autenticado = AuthenticatedActor.real(7L, "Dra. Pérez",
                "ODONTOLOGO", "jti-7");
        ActorSimulado actor = ActorSimulado.real(autenticado.usuarioId(),
                autenticado.nombre(), autenticado.rol(), autenticado.jti());

        publisher.publish(AuditEvent.de("turno.creado", "turno", 44L,
                actor, "fixture", Map.of(), "corr-real"));

        ActorSimulado persistido = recorder.eventos().get(0).actor();
        assertAll(
                () -> assertEquals(7L, persistido.usuarioId()),
                () -> assertEquals("Dra. Pérez", persistido.nombre()),
                () -> assertEquals("ODONTOLOGO", persistido.rol()),
                () -> assertEquals("jti-7", persistido.jti()),
                () -> assertEquals("REAL", persistido.modo())
        );
    }

    @Test
    void eventoDeSobreturnoConMotivoYActor() {
        publisher.publish(AuditEvent.de("turno.sobreturno", "turno", 45L,
                ActorSimulado.system(), "override autorizado", Map.of("motivo", "urgencia"),
                "corr-sobreturno"));

        AuditEvent evento = recorder.eventos().get(0);
        assertAll(
                () -> assertEquals("turno.sobreturno", evento.tipo()),
                () -> assertEquals("override autorizado", evento.motivo()),
                () -> assertEquals("urgencia", evento.metadata().get("motivo")),
                () -> assertEquals("system", evento.actor().nombre())
        );
    }

    @Test
    void operacionRechazadaNoEmiteEventoDeConfirmacion() {
        BloqueoService bloqueos = new BloqueoService(
                mock(BloqueoRepository.class),
                mock(OcupacionRepository.class),
                new SimulatedCurrentActorProvider(),
                publisher);

        assertThrows(IllegalArgumentException.class, () -> bloqueos.crearBloqueo(
                null, null, Instant.parse("2026-12-21T13:00:00Z"),
                Instant.parse("2026-12-21T14:00:00Z"), "operacion-rechazada"));

        assertEquals(0, recorder.eventos().size());
        assertFalse(recorder.eventos().stream()
                .anyMatch(evento -> evento.tipo().equals("bloqueo.creado")));
    }

    @Test
    void denegacionDeHistoriaConTokenPublico() {
        publisher.publish(AuditEvent.de("publictoken.historia.denegado", "publictoken", null,
                new ActorSimulado("paciente", "SIMULADO"), "identidad_no_verificada",
                Map.of("jti", "jti-1", "scope", "HISTORIA", "pacienteId", "paciente-42"),
                "corr-denegacion"));

        AuditEvent evento = recorder.eventos().get(0);
        assertAll(
                () -> assertEquals("publictoken", evento.entidad()),
                () -> assertNull(evento.entidadId()),
                () -> assertEquals("SIMULADO", evento.actor().modo()),
                () -> assertEquals("HISTORIA", evento.metadata().get("scope"))
        );
    }

    @Test
    void losCuatroTiposDeOperacion() {
        List<String> tipos = List.of("turno.creado", "turno.reprogramado",
                "turno.sobreturno", "bloqueo.creado");
        for (int i = 0; i < tipos.size(); i++) {
            publisher.publish(AuditEvent.de(tipos.get(i), "operacion", (long) i,
                    ActorSimulado.recepcionista(), "fixture", Map.of(), "corr-" + i));
        }

        assertEquals(tipos, recorder.eventos().stream().map(AuditEvent::tipo).toList());
        assertEquals(4, new AuditoriaCatalogo().TIPOS.stream().filter(tipos::contains).count());
    }

    @Test
    void eventosDeUnaOperacionCompartenCorrelacionYOperacionesDistintasNo() {
        String operacion = UUID.randomUUID().toString();
        String otraOperacion = UUID.randomUUID().toString();
        ActorSimulado actor = ActorSimulado.recepcionista();

        publisher.publish(AuditEvent.de("turno.creado", "turno", 1L, actor,
                "fixture", Map.of(), operacion));
        publisher.publish(AuditEvent.de("turno.reprogramado", "turno", 2L, actor,
                "fixture", Map.of(), operacion));
        publisher.publish(AuditEvent.de("turno.creado", "turno", 3L, actor,
                "fixture", Map.of(), otraOperacion));

        assertEquals(3, recorder.eventos().size());
        assertEquals(operacion, recorder.eventos().get(0).correlationId());
        assertEquals(operacion, recorder.eventos().get(1).correlationId());
        assertNotEquals(operacion, recorder.eventos().get(2).correlationId());
    }

    @Test
    void persisteAuditoriaYSinApiDeConsulta() throws IOException {
        List<String> prohibidos = List.of("/auditoria", "/audit");
        List<String> hallazgos = new java.util.ArrayList<>();

        // El escaneo se limita a src/main/java: V003 contiene legítimamente la sentencia de
        // creación de la tabla de auditoría en src/main/resources, por lo que buscarla en resources
        // daría un falso positivo. La ausencia de endpoints /audit y /auditoria se verifica en
        // código Java; la existencia de V003 se afirma por separado.
        try (var paths = Files.walk(Path.of("src", "main", "java"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String contenido = Files.readString(path).toUpperCase(java.util.Locale.ROOT);
                for (String prohibido : prohibidos) {
                    if (contenido.contains(prohibido)) {
                        hallazgos.add(path + " -> " + prohibido);
                    }
                }
            }
        }

        assertEquals(List.of(), hallazgos);
        assertTrue(Files.exists(Path.of("src", "main", "resources", "db", "migration",
                "V003__audit_append_only_log.sql")));
        assertTrue(AuditEventPublisher.class.isAssignableFrom(JdbcAuditEventPublisher.class));
        assertDoesNotThrow(() -> Class.forName(
                "com.turnosodontologia.infrastructure.audit.AuditPersistenceConfig"));
        assertNotNull(AuditEvent.creacionTurno(1L, ActorSimulado.recepcionista(),
                "motivo", Map.of()).correlationId());
    }
}
