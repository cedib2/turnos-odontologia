package com.turnosodontologia.domain.agenda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfesionalTest {

    @Test
    void creaProfesionalConMatriculaYActivoPorDefecto() {
        Profesional profesional = new Profesional("MP-1234", "Sofía", "Aguirre");

        assertEquals("MP-1234", profesional.getMatricula());
        assertEquals("Sofía", profesional.getNombre());
        assertEquals("Aguirre", profesional.getApellido());
        assertTrue(profesional.isActivo());
    }
}
