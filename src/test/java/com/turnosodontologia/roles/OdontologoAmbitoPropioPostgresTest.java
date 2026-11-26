package com.turnosodontologia.roles;

import com.turnosodontologia.application.AccesoFueraDeAlcanceException;
import com.turnosodontologia.application.AlcanceProfesional;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.clinico.HistoriaClinica;
import com.turnosodontologia.application.clinico.HistoriaClinicaService;
import com.turnosodontologia.cumplimiento.CumplimientoFixtures;
import com.turnosodontologia.cumplimiento.CumplimientoPostgresTest;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Actor simulado odontólogo vinculado al profesional 1: con ámbito {@code propio} ve solo su agenda y solo las
 * fichas clínicas de pacientes que atiende (seed V001: agenda:leer y ficha_clinica:leer con ámbito propio).
 */
@TestPropertySource(properties = {"turnos.simulado.rol=odontologo", "turnos.simulado.profesional-id=1"})
class OdontologoAmbitoPropioPostgresTest extends CumplimientoPostgresTest {

    @Autowired
    private CumplimientoFixtures fixtures;
    @Autowired
    private HistoriaClinicaService historias;
    @Autowired
    private AlcanceProfesional alcances;
    @Autowired
    private CurrentActorProvider actores;
    @Autowired
    private com.turnosodontologia.application.clinico.AutorizacionClinica autorizacion;
    @Autowired
    private com.turnosodontologia.application.AuditEventPublisher auditoria;

    private Paciente propio;
    private Paciente ajeno;

    @BeforeEach
    void sembrar() {
        fixtures.sembrarCatalogo();
        propio = fixtures.paciente("DOC-PROPIO", "Pía");
        ajeno = fixtures.paciente("DOC-AJENO", "Ariel");
        Turno conElOdontologo = fixtures.turno(propio, "S-OD-1", 9, "recepcion");
        Turno conOtro = fixtures.turno(ajeno, "S-OD-2", 10, "recepcion");
        // El esquema se recrea en cada test: el primer profesional creado es el 1, el configurado para el actor.
        assertEquals(1L, conElOdontologo.getProfesional().getId());
        assertEquals(2L, conOtro.getProfesional().getId());
        historias.abrirFicha(propio.getId(), null);
        historias.abrirFicha(ajeno.getId(), null);
    }

    @Test
    void laAgendaQuedaRestringidaAlProfesionalVinculado() {
        AlcanceProfesional.Alcance alcance = alcances.de(actores.current(), Set.of("agenda", "turno"), "leer");

        assertFalse(alcance.todos());
        assertEquals(1L, alcance.profesionalId());
    }

    @Test
    void leeLaFichaDeSuPacienteYNoLaDeUnPacienteAjeno() {
        HistoriaClinica historia = historias.fichaDelPaciente(propio.getId()).orElseThrow();
        assertEquals(propio.getId(), historia.pacienteId());

        AccesoFueraDeAlcanceException error = assertThrows(AccesoFueraDeAlcanceException.class,
                () -> historias.fichaDelPaciente(ajeno.getId()));
        assertEquals("ficha_de_paciente_no_atendido", error.getMessage());
        // La denegación queda en el log persistente aunque la operación falle.
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auditoria_log "
                + "WHERE accion = 'clinico.acceso.denegado' AND metadata->>'ambito' = 'propio'", Integer.class));
    }

    @Test
    void sinPermisoDeLecturaNoSeRevelaSiElPacienteTieneFicha() {
        Paciente nuevo = fixtures.paciente("DOC-SINPERM", "Sara");
        com.turnosodontologia.support.ActorConmutable recepcion = new com.turnosodontologia.support.ActorConmutable();
        recepcion.usar(com.turnosodontologia.domain.auditoria.ActorSimulado.recepcionista());

        // El recepcionista (sin ficha_clinica:leer) recibe el mismo rechazo con o sin ficha.
        assertThrows(com.turnosodontologia.application.clinico.AccesoClinicoDenegadoException.class,
                () -> conActor(recepcion).fichaDelPaciente(nuevo.getId()));
        assertThrows(com.turnosodontologia.application.clinico.AccesoClinicoDenegadoException.class,
                () -> conActor(recepcion).fichaDelPaciente(propio.getId()));
    }

    private HistoriaClinicaService conActor(CurrentActorProvider actor) {
        return new HistoriaClinicaService(jdbcTemplate, actor, autorizacion, auditoria, alcances);
    }

    @Test
    void unPacienteSuyoSinFichaDevuelveVacioYUnoAjenoSeRechaza() {
        Paciente nuevo = fixtures.paciente("DOC-NUEVO", "Nadia");
        fixtures.turno(nuevo, "S-OD-3", 11, "recepcion");
        jdbcTemplate.update("UPDATE turnos SET profesional_id = 1 WHERE paciente_id = ?", nuevo.getId());
        Paciente ajenoSinFicha = fixtures.paciente("DOC-AJENO-2", "Abril");

        assertTrue(historias.fichaDelPaciente(nuevo.getId()).isEmpty());
        assertThrows(AccesoFueraDeAlcanceException.class, () -> historias.fichaDelPaciente(ajenoSinFicha.getId()));
    }
}
