package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.CancelacionTurnoService;
import com.turnosodontologia.application.CreacionTurnoService;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.ReprogramacionService;
import com.turnosodontologia.domain.agenda.Turno;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/turnos")
public class TurnoController {

    private final CreacionTurnoService creacionTurnoService;
    private final CancelacionTurnoService cancelaciones;
    private final ReprogramacionService reprogramaciones;
    private final CurrentActorProvider actores;

    public TurnoController(CreacionTurnoService creacionTurnoService, CancelacionTurnoService cancelaciones,
                           ReprogramacionService reprogramaciones, CurrentActorProvider actores) {
        this.creacionTurnoService = creacionTurnoService;
        this.cancelaciones = cancelaciones;
        this.reprogramaciones = reprogramaciones;
        this.actores = actores;
    }

    /** Resumen de un turno: no se devuelve la entidad JPA con sus relaciones. */
    public record TurnoVista(Long id, Instant inicio, Instant fin, String estado, boolean activo,
                             Long profesionalId, Long sillonId) {
        static TurnoVista de(Turno turno) {
            return new TurnoVista(turno.getId(), turno.getInicio(), turno.getFin(), turno.getEstado(),
                    turno.isActivo(), turno.getProfesional().getId(), turno.getSillon().getId());
        }
    }

    public record CancelacionRequest(String motivo) {
    }

    public record ReprogramacionRequest(Instant nuevoInicio) {
    }

    @PostMapping
    public Turno crear(@RequestBody CrearTurnoRequest request) {
        return creacionTurnoService.crearTurno(request.pacienteId(), request.profesionalId(),
                request.sillonId(), request.codigoTratamiento(), request.inicio(),
                request.estado(), request.origen());
    }

    /** Cancelación por recepción, sin ventana de 48 hs: libera el hueco y audita el motivo. */
    @PostMapping("/{id}/cancelacion")
    public TurnoVista cancelar(@PathVariable Long id, @RequestBody CancelacionRequest request) {
        return TurnoVista.de(cancelaciones.cancelar(id, request == null ? null : request.motivo()));
    }

    /** Reprogramación por recepción: valida horario, feriados y solapamiento (RN-AG-01). */
    @PostMapping("/{id}/reprogramacion")
    public TurnoVista reprogramar(@PathVariable Long id, @RequestBody ReprogramacionRequest request) {
        if (request == null || request.nuevoInicio() == null) {
            throw new IllegalArgumentException("nuevoInicio es obligatorio");
        }
        return TurnoVista.de(reprogramaciones.reprogramar(id, request.nuevoInicio(), actores.current().nombre()));
    }
}
