package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Matriz efectiva de la política de agenda sobre el seed real de V001 (roles y rol_permiso).
 */
class AgendaRbacSeedPostgresTest extends PostgresFlywayTest {

    @Autowired
    private RolPermisoRepository rolPermisos;

    private void esperar(String rol, String metodo, String ruta, boolean permitido) {
        assertEquals(permitido, new AgendaRbacPolicy(rolPermisos).permite(rol, metodo, ruta),
                rol + " " + metodo + " " + ruta);
    }

    @Test
    void recepcionistaOperaLaAgendaYSoloLeeCatalogos() {
        esperar("recepcionista", "GET", "/agenda/dia", true);
        esperar("recepcionista", "POST", "/turnos", true);
        esperar("recepcionista", "POST", "/bloqueos", true);
        esperar("recepcionista", "POST", "/sobreturnos", true);
        esperar("recepcionista", "GET", "/tratamientos", true);
        esperar("recepcionista", "POST", "/tratamientos", false);
    }

    @Test
    void odontologoLeeAgendaPeroNoLaModifica() {
        esperar("odontologo", "GET", "/agenda/semana", true);
        esperar("odontologo", "POST", "/turnos", false);
        esperar("odontologo", "POST", "/sobreturnos", false);
        esperar("odontologo", "GET", "/horarios", true);
        esperar("odontologo", "PUT", "/horarios/1", false);
    }

    @Test
    void administradorTieneAccesoCompleto() {
        esperar("administrador", "GET", "/agenda", true);
        esperar("administrador", "POST", "/turnos", true);
        esperar("administrador", "POST", "/sobreturnos", true);
        esperar("administrador", "POST", "/feriados", true);
        esperar("administrador", "DELETE", "/tratamientos/1", true);
    }

    @Test
    void auditorSoloLeeCatalogosSegunElSeed() {
        esperar("auditor", "GET", "/agenda", false);
        esperar("auditor", "GET", "/feriados", true);
        esperar("auditor", "POST", "/feriados", false);
        esperar("auditor", "POST", "/turnos", false);
    }

    @Test
    void pacienteNoAccedeALaSuperficieDeStaff() {
        esperar("paciente", "GET", "/agenda", false);
        esperar("paciente", "POST", "/turnos", false);
        esperar("paciente", "GET", "/tratamientos", false);
    }
}
