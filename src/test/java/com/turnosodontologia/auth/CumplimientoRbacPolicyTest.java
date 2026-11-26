package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CumplimientoRbacPolicyTest {

    private final RolPermisoRepository repo = mock(RolPermisoRepository.class);
    private final CumplimientoRbacPolicy politica = new CumplimientoRbacPolicy(repo);

    @Test
    void cubreCumplimientoYMetricasPeroNoPrecioNiAgenda() {
        for (String ruta : new String[]{"/cumplimiento", "/cumplimiento/dossier", "/cumplimiento/exportaciones/1/contenido",
                "/cumplimiento/respaldos/2/restore-aislado", "/metricas", "/metricas/ventana"}) {
            assertTrue(politica.cubre(ruta), ruta);
        }
        for (String ruta : new String[]{"/precio", "/agenda", "/metricasx", "/cumplimientos", "/auth/me"}) {
            assertFalse(politica.cubre(ruta), ruta);
        }
    }

    @Test
    void operacionesSobreDatosExigenExportacionCrear() {
        Optional<CumplimientoRbacPolicy.Permiso> exportar = Optional.of(CumplimientoRbacPolicy.EXPORTAR);
        assertEquals(exportar, CumplimientoRbacPolicy.requisito("POST", "/cumplimiento/exportaciones"));
        assertEquals(exportar, CumplimientoRbacPolicy.requisito("GET", "/cumplimiento/exportaciones/7/contenido"));
        assertEquals(exportar, CumplimientoRbacPolicy.requisito("POST", "/cumplimiento/respaldos"));
        assertEquals(exportar, CumplimientoRbacPolicy.requisito("POST", "/cumplimiento/respaldos/7/restore-aislado"));
        assertEquals(exportar, CumplimientoRbacPolicy.requisito("POST", "/cumplimiento/dossier/publicacion"));
        assertEquals(exportar, CumplimientoRbacPolicy.requisito("PUT", "/cumplimiento/dossier/items/OWNER_LEGAL/evidencia"));
    }

    @Test
    void lecturasDeSupervisionExigenAuditoriaLeer() {
        Optional<CumplimientoRbacPolicy.Permiso> leer = Optional.of(CumplimientoRbacPolicy.LEER_AUDITORIA);
        assertEquals(leer, CumplimientoRbacPolicy.requisito("GET", "/cumplimiento/dossier"));
        assertEquals(leer, CumplimientoRbacPolicy.requisito("GET", "/cumplimiento/exportaciones/vencidas"));
        assertEquals(leer, CumplimientoRbacPolicy.requisito("GET", "/metricas"));
        assertEquals(leer, CumplimientoRbacPolicy.requisito("GET", "/metricas/adopcion"));
    }

    @Test
    void escrituraEnMetricasNoTieneReglaYSeNiega() {
        assertEquals(Optional.empty(), CumplimientoRbacPolicy.requisito("POST", "/metricas"));
        assertFalse(politica.permite("administrador", "POST", "/metricas"));
    }

    @Test
    void pacienteYRolVacioSeNieganSinConsultarLaMatriz() {
        assertFalse(politica.permite("paciente", "GET", "/metricas"));
        assertFalse(politica.permite(" ", "GET", "/metricas"));
        assertFalse(politica.permite(null, "GET", "/metricas"));
        verifyNoInteractions(repo);
    }
}
