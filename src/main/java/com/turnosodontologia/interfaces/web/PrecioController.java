package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.precio.PrecioPublico;
import com.turnosodontologia.application.precio.PrecioPublicoService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Superficie indicativa dev/test de C-21: consulta de solo lectura del precio único (visual-design diferido). */
@RestController
@Profile({"dev", "test"})
public class PrecioController {

    private final PrecioPublicoService precios;

    public PrecioController(PrecioPublicoService precios) {
        this.precios = precios;
    }

    @GetMapping("/precio")
    public PrecioPublico consultar() {
        return precios.consultar();
    }
}
