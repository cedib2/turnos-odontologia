package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    private final ConfiguracionSchedulingService configuracion;
    private final OcupacionRepository ocupaciones;

    public DisponibilidadService(ConfiguracionSchedulingService configuracion,
                                 OcupacionRepository ocupaciones) {
        this.configuracion = configuracion;
        this.ocupaciones = ocupaciones;
    }

    public List<IntervaloDisponible> huecosLibres(Long profesionalId, Long sillonId,
                                                  LocalDate fecha, int duracionMinutos) {
        if (profesionalId == null && sillonId == null) {
            throw new IllegalArgumentException("La disponibilidad requiere profesional o sillón");
        }
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser positiva");
        }
        if (configuracion.esFeriado(fecha)) {
            return List.of();
        }

        Integer profesional = profesionalId == null ? null : profesionalId.intValue();
        List<HorarioAtencion> horarios = configuracion.horariosDelDia(profesional, fecha);
        List<IntervaloDisponible> libres = new ArrayList<>();

        for (HorarioAtencion horario : horarios) {
            LocalTime cursor = horario.getDesde();
            while (!cursor.plus(duracionMinutos, ChronoUnit.MINUTES)
                    .isAfter(horario.getHasta())) {
                Instant inicio = fecha.atTime(cursor).atZone(configuracion.zonaHoraria()).toInstant();
                Instant fin = inicio.plus(duracionMinutos, ChronoUnit.MINUTES);
                if (!estaOcupado(profesionalId, sillonId, inicio, fin)) {
                    libres.add(new IntervaloDisponible(inicio, fin));
                }
                cursor = cursor.plus(duracionMinutos, ChronoUnit.MINUTES);
            }
        }
        return libres;
    }

    private boolean estaOcupado(Long profesionalId, Long sillonId, Instant inicio, Instant fin) {
        return ocupaciones.findActivosQueSolapan(profesionalId, sillonId, inicio, fin)
                .stream()
                .anyMatch(ocupacion -> coincideRecurso(profesionalId, sillonId, ocupacion));
    }

    private boolean coincideRecurso(Long profesionalId, Long sillonId, Ocupacion ocupacion) {
        return profesionalId != null && profesionalId.equals(ocupacion.getProfesionalId())
                || sillonId != null && sillonId.equals(ocupacion.getSillonId());
    }
}
