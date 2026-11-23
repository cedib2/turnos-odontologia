package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Superficie indicativa de waitlist (W-12): sin token de C-06 ni RBAC nuevo. */
@RestController
@RequestMapping("/lista-espera")
@Profile({"dev", "test"})
public class WaitlistController {

    private final ListaEsperaService listaEspera;
    private final RellenoWaitlistService relleno;

    public WaitlistController(ListaEsperaService listaEspera, RellenoWaitlistService relleno) {
        this.listaEspera = listaEspera;
        this.relleno = relleno;
    }

    @PostMapping
    public ResponseEntity<EntradaResponse> registrar(@RequestBody AltaWaitlist alta) {
        EntradaWaitlist entrada = listaEspera.registrar(alta);
        return ResponseEntity.status(201).body(EntradaResponse.de(entrada));
    }

    @GetMapping("/candidato/{candidatoId}")
    public List<EntradaResponse> porCandidato(@PathVariable Long candidatoId) {
        return listaEspera.porCandidato(candidatoId).stream().map(EntradaResponse::de).toList();
    }

    @PostMapping("/ofrecer/{turnoId}")
    public ResultadoRelleno ofrecer(@PathVariable Long turnoId) {
        return relleno.ofrecerTurnoLiberado(turnoId);
    }

    @PostMapping("/adjudicar/{turnoId}")
    public ResultadoRelleno adjudicar(@PathVariable Long turnoId) {
        return relleno.adjudicarTurnoLiberado(turnoId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException error) {
        return ResponseEntity.status(404).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflicto(IllegalStateException error) {
        return ResponseEntity.status(409).body(Map.of("error", error.getMessage()));
    }
}
