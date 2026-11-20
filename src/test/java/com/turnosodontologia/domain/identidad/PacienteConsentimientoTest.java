package com.turnosodontologia.domain.identidad;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacienteConsentimientoTest {

    private Paciente paciente() {
        return new Paciente(DocumentoTipo.DNI, "30123456", "Marta", "Gómez", "5491111111111");
    }

    @Test
    void registraConsentimientoConPoliticaFinalidadYCanal() {
        Paciente paciente = paciente();
        Instant fecha = Instant.parse("2026-11-20T12:00:00Z");

        paciente.registrarConsentimiento(true, fecha, "v1.0", "reserva y recordatorios", "link/QR");

        assertTrue(paciente.isConsentimientoDatos());
        assertEquals(fecha, paciente.getFechaConsentimiento());
        assertEquals("v1.0", paciente.getVersionPolitica());
        assertEquals("reserva y recordatorios", paciente.getFinalidad());
        assertEquals("link/QR", paciente.getCanal());
    }

    @Test
    void revocaConsentimientoSinBorrarNiDesactivarElPaciente() {
        Paciente paciente = paciente();
        paciente.registrarConsentimiento(true, Instant.parse("2026-01-01T00:00:00Z"),
                "v1.0", "reserva", "link/QR");

        paciente.registrarConsentimiento(false, Instant.parse("2026-11-20T12:00:00Z"),
                "v1.1", "reserva", "panel");

        assertFalse(paciente.isConsentimientoDatos());
        assertEquals("v1.1", paciente.getVersionPolitica());
        assertTrue(paciente.isActivo());
    }
}
