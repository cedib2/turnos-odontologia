package com.turnosodontologia.application.metricas;

import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(RealAuditPublisherConfig.class)
class MetricasAuditoriaPersistidaTest extends PostgresFlywayTest {

    @Autowired
    private MetricasAdopcionService servicio;

    @Test
    void laConsultaSePersisteEnElLogAppendOnly() {
        servicio.consultar(Instant.parse("2026-12-01T03:00:00Z"), Instant.parse("2027-01-01T03:00:00Z"));

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT accion, entidad, modo, actor_nombre FROM auditoria_log WHERE accion = ?",
                MetricasAdopcionService.EVENTO_CONSULTA);
        assertEquals("metricas", fila.get("entidad"));
        assertEquals("SIMULADO", fila.get("modo"));
        assertEquals("recepcionista", fila.get("actor_nombre"));
    }
}
