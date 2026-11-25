package com.turnosodontologia.depositpayments;

import com.turnosodontologia.audit.AuditoriaCatalogo;
import com.turnosodontologia.audit.AuditoriaMetadataValidator;
import com.turnosodontologia.domain.financiero.Senia;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoAuditMetadataTest {

    private final AuditoriaMetadataValidator validator = new AuditoriaMetadataValidator();
    private final AuditoriaCatalogo catalogo = new AuditoriaCatalogo();

    @Test
    void catalogoAceptaLosCincoTiposDePagoYRechazaCualquierOtro() {
        for (String tipo : Set.of("pago.solicitado", "pago.aprobado", "pago.rechazado",
                "pago.reembolsado", "pago.conciliado")) {
            catalogo.validar(tipo);
            assertTrue(AuditoriaCatalogo.TIPOS.contains(tipo));
        }
        for (String tipo : Set.of("pago.enviado", "notificacion.entregada", "caja.apertura")) {
            assertThrows(IllegalArgumentException.class, () -> catalogo.validar(tipo));
        }
    }

    @Test
    void metadataContieneSoloClavesPermitidas() {
        Senia senia = new Senia(11L, 22L, new BigDecimal("99.90"), "senia-meta-021", "clave-meta-021");
        ReflectionTestUtils.setField(senia, "id", 501L);
        Map<String, Object> metadata = PagoAuditMetadata.de(senia, "aprobado", "corr-021");

        assertEquals(Set.of("seniaId", "turnoId", "presupuestoId", "monto", "moneda",
                "estado", "externalReference", "motivo", "correlationId"), metadata.keySet());
        assertEquals(501L, metadata.get("seniaId"));
        assertEquals("99.90", metadata.get("monto"));
        assertEquals("ARS", metadata.get("moneda"));
        assertEquals("senia-meta-021", metadata.get("externalReference"));
        assertEquals("corr-021", metadata.get("correlationId"));
        validator.validar(metadata);
    }

    @Test
    void metadataOmiteReferenciasAusentesYRechazaClavesSensibles() {
        Senia senia = new Senia(null, null, new BigDecimal("10.00"), "senia-meta-022", "clave-meta-022");
        ReflectionTestUtils.setField(senia, "id", 502L);
        Map<String, Object> metadata = PagoAuditMetadata.de(senia, null, "corr-022");

        assertFalse(metadata.containsKey("turnoId"));
        assertFalse(metadata.containsKey("presupuestoId"));
        assertFalse(metadata.containsKey("motivo"));
        validator.validar(metadata);

        assertThrows(IllegalArgumentException.class, () -> validator.validar(
                Map.of("access_token", "no-versionar", "correlationId", "corr-022")));
    }

    @Test
    void cadaOperacionLlevaCorrelationIdDistinto() {
        Senia senia = new Senia(1L, null, new BigDecimal("10.00"), "senia-meta-023", "clave-meta-023");
        ReflectionTestUtils.setField(senia, "id", 503L);
        assertNotEquals(PagoAuditMetadata.de(senia, null, null).get("correlationId"),
                PagoAuditMetadata.de(senia, null, null).get("correlationId"));
    }
}
