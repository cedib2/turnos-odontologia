package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgendaRbacPolicyTest {

    private final RolPermisoRepository repo = mock(RolPermisoRepository.class);
    private final AgendaRbacPolicy politica = new AgendaRbacPolicy(repo);

    private void concede(String rol, String... recursoAccion) {
        when(repo.existsByRol_CodigoAndPermiso_RecursoAndPermiso_Accion(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    if (!rol.equals(inv.getArgument(0))) {
                        return false;
                    }
                    String clave = inv.getArgument(1) + ":" + inv.getArgument(2);
                    return Set.of(recursoAccion).contains(clave);
                });
    }

    @Test
    void cubreTodasLasRutasDeAgendaYCatalogos() {
        for (String ruta : new String[]{"/agenda", "/agenda/dia", "/turnos", "/bloqueos", "/sobreturnos",
                "/tratamientos", "/tratamientos/3", "/horarios", "/feriados/1"}) {
            assertTrue(AgendaRbacPolicy.aplicaA(ruta), ruta);
        }
        for (String ruta : new String[]{"/auth/me", "/agendas", "/turnosx", "/publico/turnos"}) {
            assertFalse(AgendaRbacPolicy.aplicaA(ruta), ruta);
        }
    }

    @Test
    void lecturaDeAgendaConAgendaLeerOTurnoLeer() {
        concede("odontologo", "agenda:leer");
        assertTrue(politica.permite("odontologo", "GET", "/agenda/dia"));
        concede("recepcionista", "turno:leer");
        assertTrue(politica.permite("recepcionista", "GET", "/agenda/semana"));
    }

    @Test
    void creacionExigeAgendaCrearYSobreturnoAgendaActualizar() {
        concede("recepcionista", "agenda:crear");
        assertTrue(politica.permite("recepcionista", "POST", "/turnos"));
        assertTrue(politica.permite("recepcionista", "POST", "/bloqueos"));
        assertFalse(politica.permite("recepcionista", "POST", "/sobreturnos"));

        concede("recepcionista", "agenda:crear", "agenda:actualizar");
        assertTrue(politica.permite("recepcionista", "POST", "/sobreturnos"));
    }

    @Test
    void escrituraDeCatalogoExigeConfiguracionYAgendaActualizar() {
        concede("auditor", "configuracion:leer");
        assertTrue(politica.permite("auditor", "GET", "/feriados"));
        assertFalse(politica.permite("auditor", "POST", "/feriados"));
        assertFalse(politica.permite("auditor", "DELETE", "/tratamientos/1"));

        concede("administrador", "configuracion:leer", "agenda:actualizar");
        assertTrue(politica.permite("administrador", "PUT", "/horarios/2"));
    }

    @Test
    void pacienteYRolVacioNuncaAccedenALaSuperficieDeStaff() {
        assertFalse(politica.permite("paciente", "GET", "/agenda"));
        assertFalse(politica.permite("PACIENTE", "GET", "/agenda"));
        assertFalse(politica.permite(null, "GET", "/agenda"));
        assertFalse(politica.permite(" ", "GET", "/agenda"));
        verifyNoInteractions(repo);
    }

    @Test
    void rutaFueraDeLaSuperficieSeDeniega() {
        concede("administrador", "agenda:leer");
        assertFalse(politica.permite("administrador", "GET", "/otra"));
    }
}
