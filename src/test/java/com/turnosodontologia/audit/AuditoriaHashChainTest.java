package com.turnosodontologia.audit;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaHashChainTest extends PostgresFlywayTest {

    private final AuditoriaCanonicalizer canonicalizer = new AuditoriaCanonicalizer();

    @org.springframework.beans.factory.annotation.Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void primeraFilaConValorGenesis() {
        publicar("turno.creado", 101L, "primera");

        Map<String, Object> fila = filas().get(0);
        assertEquals(AuditoriaCanonicalizer.GENESIS, fila.get("hash_anterior").toString().trim());
        assertEquals(64, fila.get("hash_actual").toString().trim().length());
    }

    @Test
    void filaEnlazadaASuPredecesora() {
        publicar("turno.creado", 101L, "primera");
        publicar("turno.reprogramado", 101L, "segunda");

        List<Map<String, Object>> filas = filas();
        assertEquals(filas.get(0).get("hash_actual").toString().trim(),
                filas.get(1).get("hash_anterior").toString().trim());
    }

    @Test
    void verificacionDeCadenaIntegra() {
        publicar("turno.creado", 101L, "primera");
        publicar("turno.reprogramado", 101L, "segunda");
        publicar("bloqueo.creado", 202L, "tercera");

        AuditoriaIntegridadService.Resultado resultado = integridad().verificar();
        assertTrue(resultado.valida());
        assertEquals(3, resultado.filasVerificadas());
    }

    @Test
    void cadenaValidaConSaltosDeIdYSinHuecosLogicos() {
        publicar("turno.creado", 101L, "primera");
        saltarSecuencia(3);
        publicar("turno.reprogramado", 101L, "segunda");
        saltarSecuencia(2);
        publicar("bloqueo.creado", 202L, "tercera");

        List<Long> ids = filas().stream()
                .map(fila -> ((Number) fila.get("id")).longValue())
                .toList();
        assertTrue(ids.get(1) - ids.get(0) > 1);
        assertTrue(ids.get(2) - ids.get(1) > 1);
        assertTrue(integridad().verificar().valida());
    }

    @Test
    void deteccionDeUnaAlteracionExterna() {
        publicar("turno.creado", 101L, "primera");
        publicar("turno.reprogramado", 101L, "segunda");
        Long primerId = ((Number) filas().get(0).get("id")).longValue();

        jdbcTemplate.execute("ALTER TABLE auditoria_log DISABLE TRIGGER ALL");
        try {
            jdbcTemplate.update("UPDATE auditoria_log SET motivo = 'manipulado' WHERE id = ?", primerId);
        } finally {
            jdbcTemplate.execute("ALTER TABLE auditoria_log ENABLE TRIGGER ALL");
        }

        AuditoriaIntegridadService.Resultado resultado = integridad().verificar();
        assertFalse(resultado.valida());
        assertEquals(primerId, resultado.idAfectado());
    }

    @Test
    void publicacionesConcurrentesSinBifurcarLaCadena() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<?> primera = executor.submit(() -> publicarTras(inicio, 301L, "concurrente-1"));
            Future<?> segunda = executor.submit(() -> publicarTras(inicio, 302L, "concurrente-2"));
            inicio.countDown();
            assertDoesNotThrow(() -> primera.get(30, TimeUnit.SECONDS));
            assertDoesNotThrow(() -> segunda.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        List<Map<String, Object>> filas = filas();
        assertEquals(2, filas.size());
        Long primerId = ((Number) filas.get(0).get("id")).longValue();
        Long segundoId = ((Number) filas.get(1).get("id")).longValue();
        assertNotEquals(primerId, segundoId);
        assertEquals(filas.get(0).get("hash_actual").toString().trim(),
                filas.get(1).get("hash_anterior").toString().trim());
        assertTrue(integridad().verificar().valida());
    }

    private void publicarTras(CountDownLatch inicio, Long entidadId, String motivo) {
        try {
            inicio.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        publicar("turno.creado", entidadId, motivo);
    }

    private void publicar(String tipo, Long entidadId, String motivo) {
        AuditEvent evento = AuditEvent.de(tipo, "turno", entidadId, ActorSimulado.recepcionista(),
                motivo, Map.of("origen", "test"), "corr-" + UUID.randomUUID());
        new TransactionTemplate(transactionManager).executeWithoutResult(
                estado -> new JdbcAuditEventPublisher(jdbcTemplate, canonicalizer).publish(evento));
    }

    private void saltarSecuencia(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            jdbcTemplate.execute("SELECT nextval('auditoria_log_id_seq')");
        }
    }

    private AuditoriaIntegridadService integridad() {
        return new AuditoriaIntegridadService(jdbcTemplate, canonicalizer);
    }

    private List<Map<String, Object>> filas() {
        return jdbcTemplate.queryForList("SELECT * FROM auditoria_log ORDER BY id");
    }
}
