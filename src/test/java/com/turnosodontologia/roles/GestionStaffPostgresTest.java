package com.turnosodontologia.roles;

import com.turnosodontologia.application.AlcanceProfesional;
import com.turnosodontologia.application.CancelacionTurnoService;
import com.turnosodontologia.application.ReprogramacionService;
import com.turnosodontologia.application.UsuarioService;
import com.turnosodontologia.cumplimiento.CumplimientoFixtures;
import com.turnosodontologia.cumplimiento.CumplimientoPostgresTest;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.RolRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Recepción gestiona la agenda (cancelar, reprogramar) y el vínculo usuario-profesional define el ámbito propio. */
class GestionStaffPostgresTest extends CumplimientoPostgresTest {

    @Autowired
    private CumplimientoFixtures fixtures;
    @Autowired
    private CancelacionTurnoService cancelaciones;
    @Autowired
    private ReprogramacionService reprogramaciones;
    @Autowired
    private AlcanceProfesional alcances;
    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private RolRepository roles;
    @Autowired
    private TurnoRepository turnos;
    @Autowired
    private OcupacionRepository ocupaciones;

    @BeforeEach
    void sembrar() {
        fixtures.sembrarCatalogo();
    }

    @Test
    void recepcionCancelaSinVentanaLiberaElHuecoYAuditaElInicioAnterior() {
        Turno turno = fixtures.turno(fixtures.paciente("DOC-CAN", "Cora"), "S-CAN", 9, "recepcion");

        Turno cancelado = cancelaciones.cancelar(turno.getId(), "el paciente avisó por teléfono");

        assertEquals("cancelado", cancelado.getEstado());
        assertFalse(cancelado.isActivo());
        assertTrue(ocupaciones.findByTurnoIdAndActivoTrue(turno.getId()).isEmpty());
        assertEquals(turno.getInicio().toString(), jdbcTemplate.queryForObject(
                "SELECT metadata->>'inicioAnterior' FROM auditoria_log WHERE accion = 'turno.cancelado'", String.class));
        assertThrows(IllegalStateException.class, () -> cancelaciones.cancelar(turno.getId(), "otra vez"));
        assertThrows(IllegalArgumentException.class, () -> cancelaciones.cancelar(turno.getId(), " "));
    }

    @Test
    void recepcionReprogramaDentroDelHorario() {
        Turno turno = fixtures.turno(fixtures.paciente("DOC-REP", "Rita"), "S-REP", 9, "recepcion");

        Turno movido = reprogramaciones.reprogramar(turno.getId(),
                CumplimientoFixtures.local(CumplimientoFixtures.FECHA, 11), ActorSimulado.recepcionista().nombre());

        assertEquals(CumplimientoFixtures.local(CumplimientoFixtures.FECHA, 11), turnos.findById(movido.getId())
                .orElseThrow().getInicio());
        assertEquals(1, ocupaciones.findAll().stream().filter(o -> o.isActivo()).count());
    }

    @Test
    void recepcionYAdministracionVenTodaLaAgenda() {
        assertTrue(alcances.de(ActorSimulado.recepcionista(), Set.of("agenda", "turno"), "leer").todos());
        assertTrue(alcances.de(new ActorSimulado(null, "administrador", "ADMINISTRADOR", null, "SIMULADO"),
                Set.of("agenda", "turno"), "leer").todos());
    }

    @Test
    void elOdontologoVinculadoTieneAmbitoPropioYSinVinculoSeRechaza() {
        Turno turno = fixtures.turno(fixtures.paciente("DOC-VIN", "Vera"), "S-VIN", 9, "recepcion");
        Long profesional = turno.getProfesional().getId();
        Usuario usuario = usuarios.registrar(roles.findByCodigo("odontologo").orElseThrow(),
                "odontologo.vinculo@consultorio.test", "ClaveSegura123!", ProveedorAutenticacion.LOCAL);
        ActorSimulado real = ActorSimulado.real(usuario.getId(), usuario.getEmail(), "ODONTOLOGO", "jti-1");

        assertThrows(com.turnosodontologia.application.AccesoFueraDeAlcanceException.class,
                () -> alcances.de(real, Set.of("agenda", "turno"), "leer"));

        usuarios.vincularProfesional(usuario.getId(), profesional);
        AlcanceProfesional.Alcance alcance = alcances.de(real, Set.of("agenda", "turno"), "leer");

        assertFalse(alcance.todos());
        assertEquals(profesional, alcance.profesionalId());
    }
}
