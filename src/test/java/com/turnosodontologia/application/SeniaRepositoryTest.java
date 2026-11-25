package com.turnosodontologia.application;

import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeniaRepositoryTest extends PostgresFlywayTest {

    @Autowired
    private SeniaRepository senias;

    @Test
    void persisteSeniaConPresupuestoYTurnoYLaLeePorReferencia() {
        Senia guardada = senias.saveAndFlush(new Senia(10L, 20L, new BigDecimal("125.50"),
                "senia-ref-021-a", "clave-021-a"));

        Senia leida = senias.findByExternalReference("senia-ref-021-a").orElseThrow();
        assertEquals(guardada.getId(), leida.getId());
        assertEquals(10L, leida.getTurnoId());
        assertEquals(20L, leida.getPresupuestoId());
        assertEquals(new BigDecimal("125.50"), leida.getMonto());
        assertEquals("ARS", leida.getMoneda());
        assertEquals("mercadopago", leida.getProveedor());
        assertEquals("pendiente", leida.getEstado());
        assertFalse(leida.isConciliado());
        assertTrue(leida.isActivo());
        assertTrue(senias.findByIdempotencyKey("clave-021-a").isPresent());
    }

    @Test
    void persisteSeniaSinPresupuesto() {
        senias.saveAndFlush(new Senia(30L, null, new BigDecimal("50.00"),
                "senia-ref-021-b", "clave-021-b"));

        Senia leida = senias.findByExternalReference("senia-ref-021-b").orElseThrow();
        assertEquals(30L, leida.getTurnoId());
        assertNull(leida.getPresupuestoId());
    }

    @Test
    void rechazaReferenciaDuplicadaYPuedeDesactivarseLogicamente() {
        senias.saveAndFlush(new Senia(40L, null, new BigDecimal("10.00"),
                "senia-ref-021-c", "clave-021-c"));
        assertThrows(DataIntegrityViolationException.class, () -> senias.saveAndFlush(
                new Senia(41L, null, new BigDecimal("10.00"),
                        "senia-ref-021-c", "clave-021-c-distinta")));

        Senia leida = senias.findByExternalReference("senia-ref-021-c").orElseThrow();
        leida.desactivar("recepcion");
        senias.saveAndFlush(leida);

        assertFalse(senias.findByExternalReference("senia-ref-021-c").orElseThrow().isActivo());
        assertTrue(senias.findById(leida.getId()).isPresent());
    }
}
