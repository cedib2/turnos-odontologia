package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.financiero.Senia;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStatusMapperTest {

    @ParameterizedTest
    @CsvSource({
            "pending,pendiente",
            "approved,aprobado",
            "rejected,rechazado",
            "cancelled,cancelado",
            "refunded,reembolsado",
            "charged_back,reembolsado"
    })
    void mapeaLosSeisEstadosDelProveedor(String proveedor, String esperado) {
        assertEquals(esperado, PaymentStatusMapper.estado(proveedor));
    }

    @Test
    void rechazaEstadoDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> PaymentStatusMapper.estado("en_revision"));
    }

    @Test
    void soloTransicionaDesdePendienteYRegistraInstanteObservado() {
        Instant observado = Instant.parse("2026-11-23T03:00:00Z");
        Senia senia = new Senia(1L, null, new BigDecimal("10.00"), "senia-map-021", "clave-map-021");

        assertTrue(senia.aplicarEstado("aprobado", observado));
        assertEquals("aprobado", senia.getEstado());
        assertEquals(observado, senia.getFechaAprobacion());

        assertFalse(senia.aplicarEstado("charged_back", observado.plusSeconds(60)));
        assertEquals("aprobado", senia.getEstado());
        assertEquals(observado, senia.getFechaAprobacion());
    }

    @Test
    void rechazoNoRegistraFechaDeAprobacionYEsTerminal() {
        Instant observado = Instant.parse("2026-11-23T03:00:00Z");
        Senia senia = new Senia(1L, null, new BigDecimal("10.00"), "senia-map-022", "clave-map-022");

        assertTrue(senia.aplicarEstado("rechazado", observado));
        assertNull(senia.getFechaAprobacion());
        assertFalse(senia.aplicarEstado("approved", observado.plusSeconds(60)));
        assertEquals("rechazado", senia.getEstado());
    }
}
