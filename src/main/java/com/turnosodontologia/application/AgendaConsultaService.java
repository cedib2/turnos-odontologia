package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Bloqueo;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AgendaConsultaService {

    private final TurnoRepository turnos;
    private final BloqueoRepository bloqueos;
    private final ConfiguracionSchedulingService configuracion;

    public AgendaConsultaService(TurnoRepository turnos, BloqueoRepository bloqueos,
                                 ConfiguracionSchedulingService configuracion) {
        this.turnos = turnos;
        this.bloqueos = bloqueos;
        this.configuracion = configuracion;
    }

    public List<EntradaAgenda> porDia(Long profesionalId, Long sillonId, LocalDate fecha) {
        return consultar(profesionalId, sillonId,
                fecha.atStartOfDay(configuracion.zonaHoraria()).toInstant(),
                fecha.plusDays(1).atStartOfDay(configuracion.zonaHoraria()).toInstant());
    }

    public List<EntradaAgenda> porSemana(Long profesionalId, Long sillonId, LocalDate inicio) {
        return consultar(profesionalId, sillonId,
                inicio.atStartOfDay(configuracion.zonaHoraria()).toInstant(),
                inicio.plusDays(7).atStartOfDay(configuracion.zonaHoraria()).toInstant());
    }

    public List<EntradaAgenda> consultar(Long profesionalId, Long sillonId,
                                        Instant desde, Instant hasta) {
        List<EntradaAgenda> entradas = new ArrayList<>();
        for (Turno turno : turnos.findActivosEnRango(profesionalId, sillonId, desde, hasta)) {
            entradas.add(new EntradaAgenda("turno", turno.getId(), turno.getInicio(), turno.getFin(),
                    turno.getProfesional().getId(), turno.getSillon().getId(), turno.getEstado()));
        }
        for (Bloqueo bloqueo : bloqueos.findActivosEnRango(profesionalId, sillonId, desde, hasta)) {
            entradas.add(new EntradaAgenda("bloqueo", bloqueo.getId(), bloqueo.getDesde(),
                    bloqueo.getHasta(), bloqueo.getProfesionalId(), bloqueo.getSillonId(),
                    bloqueo.getMotivo()));
        }
        entradas.sort(Comparator.comparing(EntradaAgenda::inicio)
                .thenComparing(EntradaAgenda::tipo));
        return entradas;
    }
}
