package com.turnosodontologia.audit;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditoriaActorSnapshotTest extends PostgresFlywayTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final AuditoriaCanonicalizer canonicalizer = new AuditoriaCanonicalizer();

    @BeforeEach
    void seedUsuarios() {
        for (Long id : List.of(7L, 9L, 88L)) {
            jdbc.update("INSERT INTO usuarios (id, rol_id, email) VALUES (?, 1, ?) " +
                            "ON CONFLICT (id) DO NOTHING",
                    id, "audit-" + id + "@test.local");
        }
    }

    @Test
    void actorRealPersistido() {
        publicar(AuditEvent.de("turno.creado", "turno", 501L,
                ActorSimulado.real(7L, "Dra. Pérez", "ODONTOLOGO", "jti-7"),
                "creado", Map.of(), "corr-real"));

        Map<String, Object> fila = filas().get(0);
        assertEquals(7L, ((Number) fila.get("actor_id")).longValue());
        assertEquals("Dra. Pérez", fila.get("actor_nombre"));
        assertEquals("ODONTOLOGO", fila.get("actor_rol"));
        assertEquals("jti-7", fila.get("actor_jti"));
        assertEquals("REAL", fila.get("modo"));
    }

    @Test
    void modosDelProveedor() {
        publicar(AuditEvent.de("turno.creado", "turno", 502L,
                ActorSimulado.recepcionista(), "simulado", Map.of(), "corr-simulado"));
        publicar(AuditEvent.de("turno.creado", "turno", 503L,
                ActorSimulado.system(), "sistema", Map.of(), "corr-sistema"));
        publicar(AuditEvent.de("turno.creado", "turno", 504L,
                ActorSimulado.pacienteSinUsuario(), "paciente", Map.of(), "corr-paciente"));

        List<Map<String, Object>> filas = filas();
        assertEquals("SIMULADO", filas.get(0).get("modo"));
        assertNull(filas.get(0).get("actor_id"));
        assertEquals("SYSTEM", filas.get(1).get("modo"));
        assertNull(filas.get(1).get("actor_id"));
        assertEquals("PACIENTE", filas.get(2).get("modo"));
        assertNull(filas.get(2).get("actor_id"));
    }

    @Test
    void instantaneaAnteCambiosPosteriores() {
        ActorSimulado original = ActorSimulado.real(9L, "Nombre original", "ODONTOLOGO", "jti-9");
        publicar(AuditEvent.de("turno.creado", "turno", 504L,
                original, "creado", Map.of(), "corr-snapshot"));
        ActorSimulado modificado = ActorSimulado.real(9L, "Nombre modificado", "ADMIN", "jti-nuevo");

        Map<String, Object> fila = filas().get(0);
        assertEquals("Nombre original", fila.get("actor_nombre"));
        assertEquals("ODONTOLOGO", fila.get("actor_rol"));
        assertEquals("jti-9", fila.get("actor_jti"));
        assertEquals("Nombre modificado", modificado.nombre());
        assertNull(fila.get("ip"));
    }

    private void publicar(AuditEvent evento) {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                new JdbcAuditEventPublisher(jdbc, canonicalizer).publish(evento));
    }

    private List<Map<String, Object>> filas() {
        return jdbc.queryForList("SELECT * FROM auditoria_log ORDER BY id");
    }
}
