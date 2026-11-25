package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;

/** Superficie de recepción para señas y reembolsos (MP-2/MP-5), sin RBAC nuevo. */
@RestController
@RequestMapping("/senias")
@Profile({"dev", "test"})
public class SeniaController {

    private final SeniaService servicio;

    public SeniaController(SeniaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<SeniaResponse> crear(@RequestBody SolicitudRequest solicitud) {
        return ResponseEntity.ok(servicio.solicitar(
                new SolicitudSenia(solicitud.turnoId(), solicitud.presupuestoId(),
                        solicitud.monto(), solicitud.idempotencyKey()),
                ActorSimulado.recepcionista()));
    }

    @PostMapping("/{id}/reembolsar")
    public ResponseEntity<RefundResponse> reembolsar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.reembolsar(id, ActorSimulado.recepcionista()));
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

    public record SolicitudRequest(Long turnoId, Long presupuestoId, BigDecimal monto,
                                   String idempotencyKey) {
    }
}
