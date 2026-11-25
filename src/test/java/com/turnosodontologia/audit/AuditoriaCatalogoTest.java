package com.turnosodontologia.audit;

import com.turnosodontologia.application.BloqueoService;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(RealAuditPublisherConfig.class)
class AuditoriaCatalogoTest extends PostgresFlywayTest {

    private static final Instant DESDE = Instant.parse("2026-12-21T13:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-12-21T14:00:00Z");

    private final AuditoriaCanonicalizer canonicalizer = new AuditoriaCanonicalizer();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BloqueoService bloqueoService;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    private Long sillonId;

    @BeforeEach
    void preparar() {
        sillonId = sillones.saveAndFlush(new Sillon("S-CATALOGO",
                consultorios.findById(1L).orElseThrow())).getId();
    }

    @Test
    void creacionDeTurno() {
        publicar("turno.creado", "turno", 101L, "creado", Map.of("origen", "test"));

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        assertEquals("turno.creado", filas.get(0).get("accion"));
        assertEquals("turno", filas.get(0).get("entidad"));
        assertEquals(101L, ((Number) filas.get(0).get("entidad_id")).longValue());
    }

    @Test
    void reprogramacionDeTurno() {
        publicar("turno.reprogramado", "turno", 102L, "reprogramado", Map.of());

        assertEquals(1, filas().size());
        assertEquals("turno.reprogramado", filas().get(0).get("accion"));
    }

    @Test
    void sobreturnoConOverride() {
        publicar("turno.sobreturno", "turno", 103L, "sobreturno",
                Map.of("motivo", "urgencia", "actorAutorizo", "recepcionista"));

        assertEquals(1, filas().size());
        Map<?, ?> metadata = metadataDe(filas().get(0));
        assertEquals("urgencia", metadata.get("motivo"));
        assertEquals("recepcionista", metadata.get("actorAutorizo"));
    }

    @Test
    void creacionDeBloqueo() {
        bloqueoService.crearBloqueo(null, sillonId, DESDE, HASTA, "fixture-bloqueo");

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        assertEquals("bloqueo.creado", filas.get(0).get("accion"));
        assertEquals("bloqueo", filas.get(0).get("entidad"));
    }

    @Test
    void creacionDePresupuesto() {
        publicar("presupuesto.creado", "presupuesto", 201L, "creado",
                Map.of("presupuestoId", 201L, "moneda", "ARS", "total", "10.00"));

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        assertEquals("presupuesto.creado", filas.get(0).get("accion"));
        assertEquals("presupuesto", filas.get(0).get("entidad"));
        assertEquals(201L, ((Number) filas.get(0).get("entidad_id")).longValue());
        assertTrue(integridad().verificar().valida());
    }

    @Test
    void modificacionDePresupuesto() {
        publicar("presupuesto.modificado", "presupuesto", 202L, "anulado",
                Map.of("presupuestoId", 202L, "motivo", "anulado"));

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        assertEquals("presupuesto.modificado", filas.get(0).get("accion"));
        assertEquals("anulado", metadataDe(filas.get(0)).get("motivo"));
    }

    @Test
    void operacionRechazadaSinFila() {
        bloqueoService.crearBloqueo(null, sillonId, DESDE, HASTA, "primer-bloqueo");
        long antes = filas().size();

        assertThrows(RuntimeException.class, () -> bloqueoService.crearBloqueo(
                null, sillonId, DESDE.plusSeconds(900), HASTA, "solapado"));
        assertThrows(IllegalArgumentException.class, () -> publicar("turno.eliminado",
                "turno", 104L, "evento inventado", Map.of()));

        assertEquals(antes, filas().size());
    }

    @Test
    void denegacionDeHistoriaConTokenPublico() {
        publicar("publictoken.historia.denegado", "publictoken", null,
                "identidad_no_verificada",
                Map.of("jti", "jti-1", "scope", "HISTORIA", "pacienteId", "paciente-42"));

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        assertEquals("publictoken", filas.get(0).get("entidad"));
        assertNull(filas.get(0).get("entidad_id"));
        Map<?, ?> metadata = metadataDe(filas.get(0));
        assertEquals("jti-1", metadata.get("jti"));
        assertEquals("HISTORIA", metadata.get("scope"));
        assertEquals("paciente-42", metadata.get("pacienteId"));
        assertTrue(integridad().verificar().valida());
    }

    private void publicar(String tipo, String entidad, Long entidadId, String motivo,
                          Map<String, Object> metadata) {
        AuditEvent evento = AuditEvent.de(tipo, entidad, entidadId, ActorSimulado.recepcionista(),
                motivo, metadata, "corr-" + UUID.randomUUID());
        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                publisher().publish(evento));
    }

    private JdbcAuditEventPublisher publisher() {
        return new JdbcAuditEventPublisher(jdbc, canonicalizer, new AuditoriaCatalogo());
    }

    private AuditoriaIntegridadService integridad() {
        return new AuditoriaIntegridadService(jdbc, canonicalizer);
    }

    private Map<?, ?> metadataDe(Map<String, Object> fila) {
        return canonicalizer.leerMetadata(fila.get("metadata").toString());
    }

    private List<Map<String, Object>> filas() {
        return jdbc.queryForList("SELECT * FROM auditoria_log ORDER BY id");
    }
}
