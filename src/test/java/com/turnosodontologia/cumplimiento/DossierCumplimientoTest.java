package com.turnosodontologia.cumplimiento;

import com.turnosodontologia.application.cumplimiento.Dossier;
import com.turnosodontologia.application.cumplimiento.DossierIncompletoException;
import com.turnosodontologia.application.cumplimiento.DossierService;
import com.turnosodontologia.application.cumplimiento.ExportacionService;
import com.turnosodontologia.application.cumplimiento.RespaldoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C-18: dossier mínimo con pendientes explícitos; la evidencia faltante bloquea la publicación. */
class DossierCumplimientoTest extends CumplimientoPostgresTest {

    static final List<String> MANUALES = List.of("OWNER_LEGAL", "MARCO_NORMATIVO", "REGISTRO_RNBD_AAIP",
            "DPA_RESPONSABLE_ENCARGADO", "PROCEDIMIENTO_INTIMA_CONSTA", "POLITICA_RETENCION", "UBICACION_DATOS");

    @Autowired
    private DossierService dossier;

    @Autowired
    private ExportacionService exportaciones;

    @Autowired
    private RespaldoService respaldos;

    @Autowired
    private CumplimientoFixtures fixtures;

    @Test
    void dossierInicialListaPendientesSinInventarEvidencia() {
        Dossier inicial = dossier.descargar("revisión");

        assertEquals(12, inicial.completitud().exigidos());
        assertEquals(1, inicial.completitud().presentes(), "solo la matriz RBAC sembrada es evidencia");
        assertEquals(new BigDecimal("8.33"), inicial.completitud().porcentaje());
        assertTrue(inicial.pendientes().containsAll(MANUALES));
        assertTrue(inicial.pendientes().containsAll(List.of("LOG_AUDITORIA_INTEGRO", "EXPORTACION_COMPLETA",
                "RESPALDO_RESTORE_VERIFICADO", "CLAUSULAS_SUBPROCESADORES")));
        assertFalse(inicial.publicable());
        assertEquals(5, inicial.matrizRoles().size());
        assertTrue(inicial.matrizRoles().get("auditor").contains("auditoria:leer:todos"));
        assertEquals("(TBD) — baseline del piloto (C-01 E)", inicial.completitud().meta());
        assertTrue(inicial.completitud().formula().contains("100 ×"));
        assertEquals(List.of("dossier.descargado"), acciones());
    }

    @Test
    void evidenciaFaltanteBloqueaLaPublicacionYElIntentoQuedaAuditado() {
        DossierIncompletoException error = assertThrows(DossierIncompletoException.class,
                () -> dossier.publicar("intento"));

        assertTrue(error.pendientes().contains("OWNER_LEGAL"));
        assertEquals(List.of("dossier.publicacion_bloqueada"), acciones());
    }

    @Test
    void subprocesadorSinClausulasAaipQuedaExcluidoYElItemPendiente() {
        dossier.registrarSubprocesador("Nube AR", "Hosting región AR", "AR", "https://proveedor/aaip-198", "DPA-1");
        dossier.registrarSubprocesador("Mensajería", "WhatsApp Business API", null, null, null);

        Dossier actual = dossier.generar();

        assertEquals(List.of("Nube AR"), actual.subprocesadoresIncluidos().stream()
                .map(Dossier.Subprocesador::nombre).toList());
        assertEquals(List.of("Mensajería"), actual.subprocesadoresExcluidos().stream()
                .map(Dossier.Subprocesador::nombre).toList());
        assertTrue(actual.pendientes().contains("CLAUSULAS_SUBPROCESADORES"));
    }

    @Test
    void evidenciaManualExigeReferenciaYNoAplicaAItemsAutomaticos() {
        assertThrows(IllegalArgumentException.class, () -> dossier.registrarEvidencia("OWNER_LEGAL", " ", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> dossier.registrarEvidencia("EXPORTACION_COMPLETA", "declaración", "x"));

        Dossier.Item item = dossier.registrarEvidencia("REGISTRO_RNBD_AAIP", "Constancia RNBD expte. 1/2026", "alta");

        assertEquals("PRESENTE", item.estado());
        assertEquals("recepcionista", item.verificadoPor());
        assertEquals(List.of("dossier.evidencia_registrada"), acciones());
    }

    @Test
    void conTodaLaEvidenciaElDossierSePublica() {
        fixtures.sembrarCatalogo();
        fixtures.turno(fixtures.paciente("DOC-DOSSIER", "Dora"), "S-DOSSIER", 9, "recepcion");
        MANUALES.forEach(codigo -> dossier.registrarEvidencia(codigo, "doc-" + codigo + ".pdf", "carga"));
        dossier.registrarSubprocesador("Nube AR", "Hosting región AR", "AR", "https://proveedor/aaip-198", "DPA-1");
        exportaciones.exportar("COMPLETA", null, "dossier");
        assertEquals("COMPLETADA", respaldos.restaurarEnAislado(respaldos.respaldar("dossier").id(), "dossier")
                .estado());

        Dossier publicado = dossier.publicar("publicación");

        assertTrue(publicado.publicable());
        assertEquals(new BigDecimal("100.00"), publicado.completitud().porcentaje());
        assertTrue(publicado.items().stream().filter(i -> i.codigo().equals("LOG_AUDITORIA_INTEGRO"))
                .findFirst().orElseThrow().evidencia().contains("cadena de hash válida"));
        assertEquals("dossier.publicado", acciones().get(acciones().size() - 1));
    }

    private List<String> acciones() {
        return jdbcTemplate.queryForList(
                "SELECT accion FROM auditoria_log WHERE accion LIKE 'dossier.%' ORDER BY id", String.class);
    }
}
