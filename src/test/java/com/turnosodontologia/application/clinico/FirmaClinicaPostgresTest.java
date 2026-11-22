package com.turnosodontologia.application.clinico;

import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.support.ActorConmutable;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ActorConmutable.Config.class)
class FirmaClinicaPostgresTest extends PostgresFlywayTest {

    @Autowired
    private HistoriaClinicaService historia;

    @Autowired
    private FirmaClinicaService firmas;

    @Autowired
    private SimulatedFirmaElectronicaGateway proveedor;

    @Autowired
    private ActorConmutable actores;

    @Autowired
    private InMemoryAuditEventRecorder auditoria;

    private long ficha;
    private long profesional;

    @BeforeEach
    void preparar() {
        actores.usar(ActorConmutable.ODONTOLOGO);
        proveedor.disponible(true);
        Long paciente = jdbcTemplate.queryForObject("INSERT INTO pacientes (documento_tipo, documento_numero, nombre, "
                + "apellido, telefono) VALUES ('DNI', '27555444', 'Mara', 'Lis', '+5491122223333') RETURNING id",
                Long.class);
        profesional = profesional("MP-4455");
        ficha = historia.abrirFicha(paciente, null);
        auditoria.clear();
    }

    @Test
    void firmaCorrectaGuardaMatriculaHashYUnEventoDeAuditoria() {
        long evolucion = historia.registrarEvolucion(ficha, profesional, null, "control sin novedades", null);

        firmas.firmarEvolucion(evolucion, profesional);

        assertTrue(firmas.evolucionFirmada(evolucion));
        assertTrue(firmas.evolucionIntegra(evolucion));
        assertEquals("MP-4455", jdbcTemplate.queryForObject(
                "SELECT matricula FROM evolucion_firmas WHERE evolucion_id = ?", String.class, evolucion));
        List<AuditEvent> firmadas = eventos("clinico.evolucion.firmada");
        assertEquals(1, firmadas.size());
        assertEquals("MP-4455", firmadas.get(0).metadata().get("matricula"));
        assertEquals("REAL", firmadas.get(0).actor().modo());
    }

    @Test
    void sinMatriculaNoSeFirmaYQuedaAuditado() {
        long sinMatricula = profesional("  ");
        long evolucion = historia.registrarEvolucion(ficha, sinMatricula, null, "texto", null);

        FirmaRechazadaException error = assertThrows(FirmaRechazadaException.class,
                () -> firmas.firmarEvolucion(evolucion, sinMatricula));

        assertEquals("matricula-faltante", error.getMessage());
        assertFalse(firmas.evolucionFirmada(evolucion));
        assertEquals("matricula-faltante", eventos("clinico.evolucion.firma_rechazada").get(0).motivo());
    }

    @Test
    void soloFirmaElAutorActivoUnaVezYSobreLaVersionVigente() {
        long otro = profesional("MP-9999");
        long evolucion = historia.registrarEvolucion(ficha, profesional, null, "texto", null);

        assertThrows(FirmaRechazadaException.class, () -> firmas.firmarEvolucion(evolucion, otro));
        firmas.firmarEvolucion(evolucion, profesional);
        assertThrows(FirmaRechazadaException.class, () -> firmas.firmarEvolucion(evolucion, profesional));

        long reemplazada = historia.registrarEvolucion(ficha, profesional, null, "borrador", null);
        historia.registrarEvolucion(ficha, profesional, null, "borrador corregido", reemplazada);
        assertThrows(FirmaRechazadaException.class, () -> firmas.firmarEvolucion(reemplazada, profesional));

        jdbcTemplate.update("UPDATE profesionales SET activo = false WHERE id = ?", profesional);
        long nueva = historia.registrarEvolucion(ficha, profesional, null, "otra", null);
        assertThrows(FirmaRechazadaException.class, () -> firmas.firmarEvolucion(nueva, profesional));
        assertEquals(List.of("profesional-distinto-del-autor", "evolucion-ya-firmada", "evolucion-reemplazada",
                        "profesional-inactivo"),
                eventos("clinico.evolucion.firma_rechazada").stream().map(AuditEvent::motivo).toList());
    }

    @Test
    void modificarUnaEvolucionFirmadaCreaNuevaVersionSinFirmaYConservaLaFirmada() {
        long firmada = historia.registrarEvolucion(ficha, profesional, null, "texto firmado", null);
        firmas.firmarEvolucion(firmada, profesional);

        long nueva = historia.registrarEvolucion(ficha, profesional, null, "texto corregido", firmada);

        assertFalse(firmas.evolucionFirmada(nueva));
        assertTrue(firmas.evolucionFirmada(firmada));
        assertTrue(firmas.evolucionIntegra(firmada));
        assertEquals("texto firmado", jdbcTemplate.queryForObject(
                "SELECT texto FROM evoluciones WHERE id = ?", String.class, firmada));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT version FROM evoluciones WHERE id = ?", Integer.class, nueva));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update("UPDATE evolucion_firmas SET matricula = 'X'"));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update("DELETE FROM evolucion_firmas"));
    }

    @Test
    void consentimientoFirmadoPorElProveedorYAuditado() {
        historia.registrarConsentimiento(ficha, "tratamiento", "acepto el tratamiento");
        long consentimiento = jdbcTemplate.queryForObject("SELECT id FROM consentimientos", Long.class);

        firmas.firmarConsentimiento(consentimiento);

        assertEquals("simulado", jdbcTemplate.queryForObject(
                "SELECT proveedor FROM consentimiento_firmas WHERE consentimiento_id = ?", String.class, consentimiento));
        assertEquals(1, eventos("clinico.consentimiento.firmado").size());
        assertThrows(FirmaRechazadaException.class, () -> firmas.firmarConsentimiento(consentimiento));
    }

    @Test
    void proveedorNoDisponibleDejaElConsentimientoSinFirmarYRegistraElFallo() {
        historia.registrarConsentimiento(ficha, "datos", "acepto");
        long consentimiento = jdbcTemplate.queryForObject("SELECT id FROM consentimientos", Long.class);
        proveedor.disponible(false);

        assertThrows(FirmaRechazadaException.class, () -> firmas.firmarConsentimiento(consentimiento));

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consentimiento_firmas", Integer.class));
        assertEquals("proveedor-simulado-no-disponible",
                eventos("clinico.consentimiento.firma_fallida").get(0).motivo());
    }

    @Test
    void firmarExigePermisoClinico() {
        long evolucion = historia.registrarEvolucion(ficha, profesional, null, "texto", null);
        actores.usar(ActorConmutable.AUDITOR);

        assertThrows(AccesoClinicoDenegadoException.class, () -> firmas.firmarEvolucion(evolucion, profesional));
        assertFalse(firmas.evolucionFirmada(evolucion));
    }

    @Test
    void proveedorSimuladoSoloEnDevTestYFueraNoFirma() {
        assertEquals(List.of("dev", "test"),
                List.of(SimulatedFirmaElectronicaGateway.class.getAnnotation(Profile.class).value()));
        assertEquals(List.of("!dev & !test"),
                List.of(FirmaElectronicaNoConfiguradaGateway.class.getAnnotation(Profile.class).value()));
        assertFalse(new FirmaElectronicaNoConfiguradaGateway()
                .firmar(new FirmaElectronicaGateway.SolicitudFirma(1L, 1L, "0".repeat(64))).firmado());
    }

    private long profesional(String matricula) {
        return jdbcTemplate.queryForObject("INSERT INTO profesionales (matricula, nombre, apellido) VALUES (?, 'N', 'A') "
                + "RETURNING id", Long.class, matricula);
    }

    private List<AuditEvent> eventos(String tipo) {
        return auditoria.eventos().stream().filter(e -> e.tipo().equals(tipo)).toList();
    }
}
