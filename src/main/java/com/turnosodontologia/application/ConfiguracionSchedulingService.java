package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.infrastructure.persistence.FeriadoRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConfiguracionSchedulingService {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final TratamientoRepository tratamientos;
    private final HorarioAtencionRepository horarios;
    private final FeriadoRepository feriados;

    public ConfiguracionSchedulingService(TratamientoRepository tratamientos,
                                          HorarioAtencionRepository horarios,
                                          FeriadoRepository feriados) {
        this.tratamientos = tratamientos;
        this.horarios = horarios;
        this.feriados = feriados;
    }

    public ZoneId zonaHoraria() {
        return ZONA;
    }

    public int duracionMinutos(String codigoTratamiento) {
        return tratamientos.findByCodigo(codigoTratamiento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay duración configurada para " + codigoTratamiento))
                .getDuracionMinutos();
    }

    public boolean esFeriado(LocalDate fecha) {
        return feriados.findByFecha(fecha).isPresent();
    }

    public List<HorarioAtencion> horariosDelDia(Integer profesionalId, LocalDate fecha) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        List<HorarioAtencion> candidatos = new ArrayList<>(
                horarios.findByDiaSemanaAndProfesionalIdIsNull(diaSemana));
        if (profesionalId != null) {
            candidatos.addAll(horarios.findByDiaSemanaAndProfesionalId(diaSemana, profesionalId.longValue()));
        }
        return candidatos;
    }

    public boolean estaDentroDeHorario(Integer profesionalId, Instant inicio, Instant fin) {
        ZonedDateTime desde = inicio.atZone(ZONA);
        ZonedDateTime hasta = fin.atZone(ZONA);
        if (!desde.toLocalDate().equals(hasta.toLocalDate())) {
            return false;
        }
        List<HorarioAtencion> candidatos = horariosDelDia(profesionalId, desde.toLocalDate());
        LocalTime inicioLocal = desde.toLocalTime();
        LocalTime finLocal = hasta.toLocalTime();
        return candidatos.stream().anyMatch(horario ->
                !inicioLocal.isBefore(horario.getDesde()) && !finLocal.isAfter(horario.getHasta()));
    }
}
