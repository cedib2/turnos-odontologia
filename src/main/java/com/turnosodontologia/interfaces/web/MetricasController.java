package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.metricas.Metrica;
import com.turnosodontologia.application.metricas.MetricasService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Superficie indicativa dev/test de C-22. El período es obligatorio en toda consulta. */
@RestController
@Profile({"dev", "test"})
@RequestMapping("/metricas")
public class MetricasController {

    private final MetricasService metricas;

    public MetricasController(MetricasService metricas) {
        this.metricas = metricas;
    }

    @GetMapping
    public List<Metrica> tablero(@RequestParam Instant desde, @RequestParam Instant hasta,
                                 @RequestParam(required = false) String motivo) {
        return metricas.tablero(desde, hasta, motivo);
    }

    @GetMapping("/adopcion")
    public Metrica adopcion(@RequestParam Instant desde, @RequestParam Instant hasta,
                            @RequestParam(required = false) String motivo) {
        return metricas.adopcionOnline(desde, hasta, motivo);
    }

    @GetMapping("/ventana")
    public Metrica ventana(@RequestParam Instant desde, @RequestParam Instant hasta,
                           @RequestParam(required = false) String motivo) {
        return metricas.cumplimientoVentana(desde, hasta, motivo);
    }
}
