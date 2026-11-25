package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.metricas.MetricasAdopcionService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Consulta del tablero de adopción. Igual que la agenda, queda limitada a dev/test hasta
 * que exista el adaptador de protección de staff (deuda de Parte 1, ver C-05/C-08).
 */
@RestController
@Profile({"dev", "test"})
@RequestMapping("/metricas")
public class MetricasController {

    private final MetricasAdopcionService servicio;

    public MetricasController(MetricasAdopcionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/adopcion")
    public ResponseEntity<?> adopcion(@RequestParam(required = false) Instant desde,
                                      @RequestParam(required = false) Instant hasta) {
        try {
            return ResponseEntity.ok(Map.of("metricas", servicio.consultar(desde, hasta)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
