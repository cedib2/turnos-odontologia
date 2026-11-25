package com.turnosodontologia.application.precio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecioPublicoServiceTest {

    @Test
    void sinConfiguracionNoInventaUnaCifra() {
        PrecioPublico precio = new PrecioPublicoService("", "", "").consultar();

        assertEquals(EstadoPrecio.PENDIENTE_DEFINICION, precio.estado());
        assertNull(precio.monto());
        assertNull(precio.total());
        assertTrue(precio.pendientes().contains(PrecioPublicoService.PENDIENTE_MONTO));
    }

    @Test
    void elTotalPublicadoCoincideConElPrecioDelPlan() {
        PrecioPublico precio = new PrecioPublicoService("1000.50", "ARS", "mensual").consultar();

        assertEquals(EstadoPrecio.PUBLICADO, precio.estado());
        assertEquals(0, precio.total().compareTo(precio.monto()));
        BigDecimal adicionales = precio.incluye().stream().map(ConceptoIncluido::cargoAdicional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, adicionales.signum());
        assertTrue(precio.cargosAdicionales().isEmpty());
    }

    @Test
    void esPlanoTodoIncluidoSinDesgloseDeApi() {
        PrecioPublico precio = new PrecioPublicoService("", "", "").consultar();

        assertEquals(PrecioPublicoService.MODELO, precio.modelo());
        assertFalse(precio.desgloseCostosApi());
        List<String> codigos = precio.incluye().stream().map(ConceptoIncluido::codigo).toList();
        assertTrue(codigos.containsAll(List.of("whatsapp", "mercadopago", "arca")));
        assertTrue(precio.decision().startsWith("C-01 A5"));
    }

    @Test
    void lasComisionesDeMercadoPagoQuedanExplicitamenteTbd() {
        assertTrue(new PrecioPublicoService("", "", "").consultar().pendientes()
                .contains(PrecioPublicoService.PENDIENTE_COMISIONES));
    }

    @Test
    void configuracionParcialOInvalidaFallaAlArrancar() {
        assertThrows(IllegalStateException.class, () -> new PrecioPublicoService("1000", "", ""));
        assertThrows(IllegalStateException.class, () -> new PrecioPublicoService("0", "ARS", "mensual"));
        assertThrows(IllegalStateException.class, () -> new PrecioPublicoService("-5", "ARS", "mensual"));
        assertThrows(IllegalStateException.class, () -> new PrecioPublicoService("mil", "ARS", "mensual"));
        assertThrows(IllegalStateException.class, () -> new PrecioPublicoService("1000", "pesos", "mensual"));
    }

    @Test
    void elModeloRechazaPacksODesgloses() {
        ConceptoIncluido pack = new ConceptoIncluido("pack", "pack sorpresa", BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new PrecioPublico(EstadoPrecio.PENDIENTE_DEFINICION,
                PrecioPublicoService.MODELO, null, null, null, null, List.of(), List.of(pack), false,
                List.of(), PrecioPublicoService.DECISION));
        assertThrows(IllegalArgumentException.class, () -> new PrecioPublico(EstadoPrecio.PENDIENTE_DEFINICION,
                PrecioPublicoService.MODELO, null, null, null, null, List.of(), List.of(), true,
                List.of(), PrecioPublicoService.DECISION));
        assertThrows(IllegalArgumentException.class,
                () -> new ConceptoIncluido("whatsapp", "extra", BigDecimal.ONE));
    }

    @Test
    void totalDistintoDelMontoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new PrecioPublico(EstadoPrecio.PUBLICADO,
                PrecioPublicoService.MODELO, BigDecimal.TEN, "ARS", "mensual", BigDecimal.ONE, List.of(),
                List.of(), false, List.of(), PrecioPublicoService.DECISION));
    }
}
