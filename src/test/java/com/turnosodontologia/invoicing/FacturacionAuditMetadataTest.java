package com.turnosodontologia.invoicing;

import com.turnosodontologia.audit.AuditoriaCatalogo;
import com.turnosodontologia.audit.AuditoriaMetadataValidator;
import com.turnosodontologia.domain.financiero.Comprobante;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AR-12, AR-14: catálogo aprobado, metadata cerrada y errores saneados. */
class FacturacionAuditMetadataTest {

    private static final List<String> TIPOS = List.of("factura.emitida", "factura.rechazada",
            "factura.conciliada", "factura.anulada", "nota_credito.emitida");

    @Test
    void elCatalogoAceptaLosCincoTiposAprobadosPorElPO() {
        AuditoriaCatalogo catalogo = new AuditoriaCatalogo();
        for (String tipo : TIPOS) {
            assertTrue(AuditoriaCatalogo.TIPOS.contains(tipo));
            catalogo.validar(tipo);
        }
        assertThrows(IllegalArgumentException.class, () -> catalogo.validar("factura.pagada"));
    }

    @Test
    void laMetadataSoloContieneClavesPermitidasYSinCaeNiDatosPersonales() {
        Comprobante comprobante = comprobante();
        Map<String, Object> metadata = FacturacionAuditMetadata.de(
                comprobante, "emision", "corr-1");

        assertEquals(Set.of("comprobanteId", "seniaId", "presupuestoId", "turnoId", "monto",
                "moneda", "tipo", "estado", "motivo", "correlationId"), metadata.keySet());
        assertEquals("100.00", metadata.get("monto"));
        assertEquals("ARS", metadata.get("moneda"));
        assertEquals("factura", metadata.get("tipo"));
        assertFalse(metadata.toString().contains("CAE-TEST"));
        assertFalse(metadata.toString().contains("DNI"));
        assertFalse(metadata.toString().contains("APP_USR"));
        assertTrue(new AuditoriaMetadataValidator().validar(metadata) == metadata);
    }

    @Test
    void cadaOperacionGeneraSuPropioCorrelationId() {
        Comprobante comprobante = comprobante();
        String primero = FacturacionAuditMetadata.de(comprobante, "emision", null)
                .get("correlationId").toString();
        String segundo = FacturacionAuditMetadata.de(comprobante, "reintento", null)
                .get("correlationId").toString();
        assertFalse(primero.equals(segundo));
    }

    @Test
    void elSaneadorDescartaCualquierTextoQueNoSeaUnCodigoControlado() {
        assertEquals("ERROR_PROVEEDOR", ErrorSaneador.sanear("error_proveedor"));
        assertEquals(ErrorSaneador.GENERICO,
                ErrorSaneador.sanear("Bearer APP_USR-token http://arca.example?dni=123"));
        assertEquals(ErrorSaneador.GENERICO, ErrorSaneador.sanear(null));
    }

    private Comprobante comprobante() {
        Comprobante comprobante = new Comprobante(10L, 20L, 30L,
                new BigDecimal("100.00"), "Seña odontológica");
        ReflectionTestUtils.setField(comprobante, "id", 99L);
        comprobante.emitir("CAE-TEST-ABC", java.time.Instant.now());
        return comprobante;
    }
}
