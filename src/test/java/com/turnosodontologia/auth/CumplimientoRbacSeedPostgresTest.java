package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Matriz efectiva de la política de cumplimiento sobre el seed real de V001: el administrador opera,
 * el auditor solo lee y el resto del staff no accede.
 */
class CumplimientoRbacSeedPostgresTest extends PostgresFlywayTest {

    @Autowired
    private RolPermisoRepository rolPermisos;

    private void esperar(String rol, String metodo, String ruta, boolean permitido) {
        assertEquals(permitido, new CumplimientoRbacPolicy(rolPermisos).permite(rol, metodo, ruta),
                rol + " " + metodo + " " + ruta);
    }

    @Test
    void administradorExportaRespaldaYGestionaElDossier() {
        esperar("administrador", "POST", "/cumplimiento/exportaciones", true);
        esperar("administrador", "GET", "/cumplimiento/exportaciones/1/contenido", true);
        esperar("administrador", "POST", "/cumplimiento/respaldos/1/restore-aislado", true);
        esperar("administrador", "PUT", "/cumplimiento/dossier/items/OWNER_LEGAL/evidencia", true);
        esperar("administrador", "GET", "/cumplimiento/dossier", true);
        esperar("administrador", "GET", "/metricas", true);
    }

    @Test
    void auditorLeeDossierVencidasYMetricasSinOperar() {
        esperar("auditor", "GET", "/cumplimiento/dossier", true);
        esperar("auditor", "GET", "/cumplimiento/exportaciones/vencidas", true);
        esperar("auditor", "GET", "/metricas/ventana", true);
        esperar("auditor", "GET", "/cumplimiento/exportaciones/1/contenido", false);
        esperar("auditor", "POST", "/cumplimiento/respaldos", false);
        esperar("auditor", "POST", "/cumplimiento/dossier/publicacion", false);
    }

    @Test
    void recepcionistaOdontologoYPacienteNoAcceden() {
        for (String rol : new String[]{"recepcionista", "odontologo", "paciente"}) {
            esperar(rol, "GET", "/metricas", false);
            esperar(rol, "GET", "/cumplimiento/dossier", false);
            esperar(rol, "POST", "/cumplimiento/exportaciones", false);
        }
    }
}
