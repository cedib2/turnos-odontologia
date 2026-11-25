package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.financiero.Comprobante;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AR-2: entidad sobre AuditableEntity, borrado lógico y referencias opcionales sin FK. */
class ComprobantePersistenceTest extends PostgresFlywayTest {

    @Autowired
    private ComprobanteRepository comprobantes;

    @Test
    void guardaConReferenciasOpcionalesYLasConserva() {
        Long seniaId = crearSenia();
        Comprobante guardado = comprobantes.saveAndFlush(new Comprobante(
                seniaId, 987654L, 123456L, new BigDecimal("120.50"), "Seña odontológica"));

        Comprobante leido = comprobantes.findBySeniaIdAndTipo(seniaId, Comprobante.TIPO_FACTURA)
                .orElseThrow();
        assertEquals(guardado.getId(), leido.getId());
        assertEquals(seniaId, leido.getSeniaId());
        assertEquals(987654L, leido.getPresupuestoId());
        assertEquals(123456L, leido.getTurnoId());
        assertEquals(Comprobante.MONEDA, leido.getMoneda());
        assertEquals(Comprobante.PENDIENTE, leido.getEstado());
        assertTrue(leido.isActivo());
        assertEquals("sistema", leido.getCreatedBy());
    }

    @Test
    void rechazaUnSegundoComprobanteDelMismoTipoParaLaMismaSenia() {
        Long seniaId = crearSenia();
        comprobantes.saveAndFlush(new Comprobante(
                seniaId, null, null, new BigDecimal("50.00"), "Seña odontológica"));

        assertThrows(DataIntegrityViolationException.class, () -> comprobantes.saveAndFlush(
                new Comprobante(seniaId, null, null, new BigDecimal("50.00"), "Otra seña")));
        assertEquals(1, comprobantes.count());
    }

    @Test
    void conservaReferenciasInexistentesPorqueNoHayForeignKey() {
        Long seniaId = crearSenia();
        Comprobante guardado = comprobantes.saveAndFlush(new Comprobante(
                seniaId, 99999999L, 88888888L, new BigDecimal("75.00"), "Seña odontológica"));

        assertEquals(99999999L, guardado.getPresupuestoId());
        assertEquals(88888888L, guardado.getTurnoId());
    }

    private Long crearSenia() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update("INSERT INTO senias (external_reference, idempotency_key, monto, "
                        + "moneda, estado, proveedor) VALUES (?, ?, 100, 'ARS', 'aprobado', 'mercadopago')",
                "senia-persist-" + sufijo, "idem-persist-" + sufijo);
        return jdbcTemplate.queryForObject("SELECT id FROM senias WHERE external_reference = ?",
                Long.class, "senia-persist-" + sufijo);
    }
}
