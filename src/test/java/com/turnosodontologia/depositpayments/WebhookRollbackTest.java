package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(PagoFailingAuditConfig.class)
class WebhookRollbackTest extends PostgresFlywayTest {

    @Autowired
    private SeniaService servicio;

    @Autowired
    private SeniaRepository senias;

    @Test
    void siFallaLaAuditoriaNoQuedanClaveNiCambioDeEstadoNiFila() {
        Senia seña = senias.saveAndFlush(new Senia(1L, null, new BigDecimal("10.00"),
                "senia-rollback-021", "clave-rollback-021"));

        assertThrows(IllegalStateException.class, () -> servicio.procesarWebhook(
                new WebhookSenia(seña.getExternalReference(), "approved", "pay-rollback", "ts-rollback"),
                "req-rollback-021", ActorSimulado.system()));

        assertEquals("pendiente", senias.findById(seña.getId()).orElseThrow().getEstado());
        Integer idempotencias = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = 'pago.webhook'",
                Integer.class);
        assertEquals(0, idempotencias);
        Integer filas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log", Integer.class);
        assertEquals(0, filas);
        assertFalse(senias.findById(seña.getId()).orElseThrow().isConciliado());
    }
}
