package com.turnosodontologia.domain.financiero;

import com.turnosodontologia.audit.AuditoriaMetadataValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresupuestoAuditMetadataTest {

    @Test
    void metadataPermitidaConTurno() {
        Map<String, Object> metadata = PresupuestoAuditMetadata.de(
                11L, 22L, new BigDecimal("15.50"), 2, "anulado");

        assertDoesNotThrow(() -> new AuditoriaMetadataValidator().validar(metadata));
        assertEquals(11L, metadata.get("presupuestoId"));
        assertEquals(22L, metadata.get("turnoId"));
        assertEquals("ARS", metadata.get("moneda"));
        assertEquals(2, metadata.get("cantidadItems"));
        assertEquals("anulado", metadata.get("motivo"));
        assertFalse(metadata.toString().contains("paciente"));
    }

    @Test
    void metadataSinTurnoOmiteLaClave() {
        Map<String, Object> metadata = PresupuestoAuditMetadata.de(
                12L, null, new BigDecimal("0.00"), 1, "");

        assertDoesNotThrow(() -> new AuditoriaMetadataValidator().validar(metadata));
        assertFalse(metadata.containsKey("turnoId"));
        assertFalse(metadata.containsKey("motivo"));
        assertTrue(metadata.containsKey("total"));
    }
}
