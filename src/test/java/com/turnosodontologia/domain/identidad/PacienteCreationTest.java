package com.turnosodontologia.domain.identidad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacienteCreationTest {

    @Test
    void creaPacienteConDocumentoTelefonoYAtributosDeAuditoria() {
        Paciente paciente = new Paciente(
                DocumentoTipo.DNI, "12345678", "Ana", "Pérez", "5491112345678");

        assertEquals(DocumentoTipo.DNI, paciente.getDocumentoTipo());
        assertEquals("12345678", paciente.getDocumentoNumero());
        assertEquals("Ana", paciente.getNombre());
        assertEquals("Pérez", paciente.getApellido());
        assertEquals("5491112345678", paciente.getTelefono());
        assertNotNull(paciente.getCreatedAt());
        assertTrue(paciente.isActivo());
    }

    @Test
    void creaPacienteConCamposOpcionalesNulos() {
        Paciente paciente = new Paciente(
                DocumentoTipo.CUIT, "20-12345678-9", "Empresa", "Sa", "5491199999999");

        assertEquals(DocumentoTipo.CUIT, paciente.getDocumentoTipo());
        assertNull(paciente.getFechaNacimiento());
        assertNull(paciente.getEmail());
        assertNull(paciente.getDireccion());
    }
}
