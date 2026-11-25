package com.turnosodontologia.audit;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.metricas.MetricasAdopcionService;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(MetricasTransaccionTest.Config.class)
class MetricasTransaccionTest extends PostgresFlywayTest {

    private static final List<Boolean> TRANSACCION_ACTIVA = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        AuditEventPublisher publicadorQueRegistraTransaccion() {
            return evento -> TRANSACCION_ACTIVA.add(
                    TransactionSynchronizationManager.isActualTransactionActive());
        }
    }

    @Autowired
    private MetricasAdopcionService servicio;

    @Test
    void elEventoSePublicaDentroDeUnaTransaccionActiva() {
        TRANSACCION_ACTIVA.clear();

        servicio.consultar(Instant.parse("2026-12-01T03:00:00Z"), Instant.parse("2027-01-01T03:00:00Z"));

        assertEquals(List.of(true), TRANSACCION_ACTIVA);
    }
}
