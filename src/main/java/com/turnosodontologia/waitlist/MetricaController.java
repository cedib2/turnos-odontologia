package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.waitlist.MetricaOcupacion;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

/** Superficie indicativa de métricas (W-12): cifra, metodología y período, sin meta inventada. */
@RestController
@RequestMapping("/metricas")
@Profile({"dev", "test"})
public class MetricaController {

    private final MetricaOcupacionService metricas;

    public MetricaController(MetricaOcupacionService metricas) {
        this.metricas = metricas;
    }

    @PostMapping("/publicar/{sillonId}/{periodo}")
    public ResponseEntity<MetricaResponse> publicar(@PathVariable Long sillonId,
                                                    @PathVariable String periodo) {
        MetricaOcupacion metrica = metricas.publicar(sillonId, periodo);
        if (metrica == null) {
            return ResponseEntity.status(422).body(null);
        }
        return ResponseEntity.status(201).body(MetricaResponse.de(metrica));
    }

    @GetMapping("/{sillonId}/{periodo}")
    public ResponseEntity<MetricaResponse> consultar(@PathVariable Long sillonId,
                                                     @PathVariable String periodo) {
        return ResponseEntity.of(java.util.Optional.ofNullable(
                MetricaResponse.de(metricas.consultar(sillonId, periodo))));
    }

    @PostMapping("/inasistencia/{turnoId}")
    public ResponseEntity<Long> registrarInasistencia(@PathVariable Long turnoId) {
        Turno turno = metricas.registrarInasistencia(turnoId);
        return ResponseEntity.status(201).body(turno.getId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException error) {
        return ResponseEntity.status(404).body(Map.of("error", error.getMessage()));
    }
}
