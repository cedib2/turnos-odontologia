package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.precio.PrecioPublico;
import com.turnosodontologia.application.precio.PrecioPublicoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública y de solo lectura del precio del plan (C-21, US-016). */
@RestController
public class PrecioController {

    private final PrecioPublicoService servicio;

    public PrecioController(PrecioPublicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/precio")
    public PrecioPublico consultar() {
        return servicio.consultar();
    }
}
