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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaTrazabilidadTest extends PostgresFlywayTest {

    private final AuditoriaCanonicalizer canonicalizer = new AuditoriaCanonicalizer();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void seedUsuarios() {
        jdbc.update("INSERT INTO usuarios (id, rol_id, email) VALUES (?, 3, ?) " +
                        "ON CONFLICT (id) DO NOTHING",
                11L, "audit-11@test.local");
    }

    @Test
    void filaConTodosLosCamposDeTrazabilidad() {
        AuditEvent evento = AuditEvent.conTrazabilidad(
                "bloqueo.creado", "bloqueo", 601L,
                ActorSimulado.real(11L, "Dr. López", "ODONTOLOGO", "jti-11"),
                "mantencion", Map.of("profesionalId", 11L),
                "192.168.1.20", "Chrome/120", "corr-trazabilidad");
        publicar(evento);

        Map<String, Object> fila = filas().get(0);
        assertEquals("bloqueo.creado", fila.get("accion"));
        assertEquals("bloqueo", fila.get("entidad"));
        assertEquals("mantencion", fila.get("motivo"));
        assertEquals("192.168.1.20", fila.get("ip").toString());
        assertEquals("Chrome/120", fila.get("dispositivo"));
        assertEquals("corr-trazabilidad", fila.get("correlation_id"));
        assertTrue(new AuditoriaIntegridadService(jdbc, canonicalizer).verificar().valida());
    }

    @Test
    void correlacionDistintaEntreOperaciones() {
        publicar(AuditEvent.de("turno.creado", "turno", 602L,
                ActorSimulado.recepcionista(), "primera", Map.of(), "corr-a"));
        publicar(AuditEvent.de("turno.reprogramado", "turno", 602L,
                ActorSimulado.recepcionista(), "segunda", Map.of(), "corr-b"));

        List<Map<String, Object>> filas = filas();
        assertEquals("corr-a", filas.get(0).get("correlation_id"));
        assertEquals("corr-b", filas.get(1).get("correlation_id"));
        assertNotEquals(filas.get(0).get("correlation_id"), filas.get(1).get("correlation_id"));
    }

    private void publicar(AuditEvent evento) {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                new JdbcAuditEventPublisher(jdbc, canonicalizer).publish(evento));
    }

    private List<Map<String, Object>> filas() {
        return jdbc.queryForList("SELECT * FROM auditoria_log ORDER BY id");
    }
}
