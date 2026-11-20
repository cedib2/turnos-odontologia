package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.CreacionTurnoService;
import com.turnosodontologia.domain.agenda.Turno;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/turnos")
public class TurnoController {

    private final CreacionTurnoService creacionTurnoService;

    public TurnoController(CreacionTurnoService creacionTurnoService) {
        this.creacionTurnoService = creacionTurnoService;
    }

    @PostMapping
    public Turno crear(@RequestBody CrearTurnoRequest request) {
        return creacionTurnoService.crearTurno(request.pacienteId(), request.profesionalId(),
                request.sillonId(), request.codigoTratamiento(), request.inicio(),
                request.estado(), request.origen());
    }
}
