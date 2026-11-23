package com.turnosodontologia.invoicing;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

/** Superficie de recepción de C-16 (AR-13): dev/test, sin RBAC nuevo. */
@RestController
@RequestMapping("/comprobantes")
@Profile({"dev", "test"})
public class ComprobanteController {

    private final ComprobanteService servicio;

    public ComprobanteController(ComprobanteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ComprobanteResponse> emitir(@RequestBody SolicitudRequest solicitud) {
        return ResponseEntity.ok(servicio.emitir(solicitud.seniaId(), ActorSimulado.recepcionista()));
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<ComprobanteResponse> reintentar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.reintentar(id, ActorSimulado.recepcionista()));
    }

    @PostMapping("/{id}/nota-credito")
    public ResponseEntity<ComprobanteResponse> notaCredito(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.notaCredito(id, ActorSimulado.recepcionista()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> solicitudInvalida(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("motivo", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> operacionRechazada(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("motivo", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("motivo", e.getMessage()));
    }

    public record SolicitudRequest(Long seniaId) {
    }
}
