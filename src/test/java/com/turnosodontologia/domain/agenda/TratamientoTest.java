package com.turnosodontologia.domain.agenda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TratamientoTest {

    @Test
    void aceptaDuracionConfigurablePositiva() {
        Tratamiento tratamiento = new Tratamiento("LIMPIEZA", "Limpieza", 30);

        assertEquals(30, tratamiento.getDuracionMinutos());
    }

    @Test
    void rechazaDuracionCeroONegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tratamiento("CERO", "Sin duración", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Tratamiento("NEGATIVA", "Duración inválida", -5));
    }
}
