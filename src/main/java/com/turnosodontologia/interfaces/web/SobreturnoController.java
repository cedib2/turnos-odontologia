package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.SobreturnoService;
import com.turnosodontologia.domain.agenda.Turno;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/sobreturnos")
public class SobreturnoController {

    private final SobreturnoService sobreturnoService;

    public SobreturnoController(SobreturnoService sobreturnoService) {
        this.sobreturnoService = sobreturnoService;
    }

    @PostMapping
    public Turno crear(@RequestBody SobreturnoRequest request) {
        return sobreturnoService.reprogramarConSobreturno(request.turnoId(),
                request.nuevoInicio(), request.motivo(), request.actor());
    }
}
