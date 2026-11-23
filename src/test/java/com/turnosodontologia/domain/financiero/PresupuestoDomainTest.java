package com.turnosodontologia.domain.financiero;

import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresupuestoDomainTest {

    @Test
    void calculaTotalConVariosItems() {
        Presupuesto presupuesto = nuevoPresupuesto();
        presupuesto.agregarItem(tratamiento(), 2, new BigDecimal("10.00"), "Limpieza");
        presupuesto.agregarItem(tratamiento(), 1, new BigDecimal("5.50"), "Control");

        assertEquals(0, new BigDecimal("25.50").compareTo(presupuesto.getTotal()));
        assertEquals(2, presupuesto.getItems().size());
    }

    @Test
    void redondeaPreciosYTotalConHalfUpADosDecimales() {
        Presupuesto presupuesto = nuevoPresupuesto();
        presupuesto.agregarItem(tratamiento(), 1, new BigDecimal("2.345"), "Redondeo");

        assertEquals(0, new BigDecimal("2.35").compareTo(presupuesto.getItems().get(0).getPrecio()));
        assertEquals(0, new BigDecimal("2.35").compareTo(presupuesto.getTotal()));
    }

    @Test
    void rechazaItemsInvalidos() {
        Presupuesto presupuesto = nuevoPresupuesto();

        assertThrows(IllegalArgumentException.class, () ->
                presupuesto.agregarItem(tratamiento(), 0, BigDecimal.ONE, "Cantidad cero"));
        assertThrows(IllegalArgumentException.class, () ->
                presupuesto.agregarItem(tratamiento(), 1, new BigDecimal("-1.00"), "Precio negativo"));
        assertThrows(IllegalArgumentException.class, () ->
                presupuesto.agregarItem(tratamiento(), 1, BigDecimal.ONE, "  "));
        assertTrue(presupuesto.getItems().isEmpty());
    }

    @Test
    void anularDesactivaYBloqueaTodaModificacion() {
        Presupuesto presupuesto = nuevoPresupuesto();
        presupuesto.agregarItem(tratamiento(), 1, BigDecimal.TEN, "Inicial");

        presupuesto.anular("qa");

        assertEquals(Presupuesto.ESTADO_ANULADO, presupuesto.getEstado());
        assertFalse(presupuesto.isActivo());
        assertEquals(0, new BigDecimal("10.00").compareTo(presupuesto.getTotal()));
        assertEquals(1, presupuesto.getItems().size());
        assertThrows(PresupuestoNoModificableException.class, () ->
                presupuesto.agregarItem(tratamiento(), 1, BigDecimal.ONE, "Post anulación"));
        assertThrows(PresupuestoNoModificableException.class, presupuesto::limpiarItems);
    }

    private Presupuesto nuevoPresupuesto() {
        return new Presupuesto(new Paciente(DocumentoTipo.DNI, "1", "Nombre", "Apellido", "555"), null);
    }

    private Tratamiento tratamiento() {
        return new Tratamiento("LIMPIEZA", "Limpieza", 30);
    }
}
