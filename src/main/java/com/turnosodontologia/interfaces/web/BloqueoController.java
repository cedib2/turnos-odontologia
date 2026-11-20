package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.BloqueoService;
import com.turnosodontologia.domain.agenda.Bloqueo;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/bloqueos")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    public BloqueoController(BloqueoService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    @PostMapping
    public Bloqueo crear(@RequestBody CrearBloqueoRequest request) {
        return bloqueoService.crearBloqueo(request.profesionalId(), request.sillonId(),
                request.desde(), request.hasta(), request.motivo());
    }
}
