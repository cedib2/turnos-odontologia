package com.turnosodontologia.cumplimiento;

import com.turnosodontologia.application.CreacionTurnoService;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Datos de agenda mínimos para los tests de la Parte 4. */
@Component
public class CumplimientoFixtures {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    public static final LocalDate FECHA = LocalDate.of(2026, 12, 21);
    public static final String TRATAMIENTO = "TRAT-P4";

    @Autowired
    private PacienteRepository pacientes;
    @Autowired
    private ProfesionalRepository profesionales;
    @Autowired
    private SillonRepository sillones;
    @Autowired
    private ConsultorioRepository consultorios;
    @Autowired
    private TratamientoRepository tratamientos;
    @Autowired
    private HorarioAtencionRepository horarios;
    @Autowired
    private CreacionTurnoService creacion;

    public void sembrarCatalogo() {
        horarios.saveAndFlush(new HorarioAtencion(null, FECHA.getDayOfWeek().getValue(),
                LocalTime.of(8, 0), LocalTime.of(20, 0)));
        tratamientos.saveAndFlush(new Tratamiento(TRATAMIENTO, "Fixture parte 4", 30));
    }

    public Paciente paciente(String documento, String nombre) {
        return pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI, documento, nombre, "Fixture", "555"));
    }

    public Turno turno(Paciente paciente, String sillon, int hora, String origen) {
        Long profesional = profesionales.saveAndFlush(new Profesional("MP-P4-" + System.nanoTime(),
                "Profesional", "Fixture")).getId();
        Sillon creado = sillones.saveAndFlush(new Sillon(sillon, consultorios.findById(1L).orElseThrow()));
        return creacion.crearTurno(paciente.getId(), profesional, creado.getId(), TRATAMIENTO,
                FECHA.atTime(hora, 0).atZone(ZONA).toInstant(), "fixture-estado", origen);
    }

    public static Instant local(LocalDate fecha, int hora) {
        return fecha.atTime(hora, 0).atZone(ZONA).toInstant();
    }
}
