package com.turnosodontologia.publicbooking;

import com.turnosodontologia.audit.AuditoriaMetadataValidator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicAuditMetadataTest {

    @Test
    void metadataNoContieneClavesProhibidasNiElSecretoDeIdempotencia() {
        String clave = "idempotency-key-sensible-123456";
        Map<String, Object> metadata = PublicAuditMetadata.de(11L, clave, "reprogramación");

        assertDoesNotThrow(() -> new AuditoriaMetadataValidator().validar(metadata));
        assertFalse(metadata.toString().contains(clave));
        assertTrue(metadata.containsKey("idempotencyKeyHash"));
        assertTrue(metadata.containsValue("link"));
        assertEqualsPACIENTE(metadata);
    }

    private static void assertEqualsPACIENTE(Map<String, Object> metadata) {
        org.junit.jupiter.api.Assertions.assertEquals(11L, metadata.get("turnoId"));
    }
}
