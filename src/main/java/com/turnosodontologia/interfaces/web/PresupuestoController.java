package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.ItemPresupuestoSolicitud;
import com.turnosodontologia.application.PresupuestoService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/presupuestos")
public class PresupuestoController {

    private final PresupuestoService servicio;

    public PresupuestoController(PresupuestoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody PresupuestoRequest request) {
        return PresupuestoRespuestas.ejecutar(() -> PresupuestoResponse.desde(
                servicio.crear(request.pacienteId(), request.turnoId(), solicitudes(request))));
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return PresupuestoRespuestas.ejecutar(() -> servicio.listar().stream()
                .map(PresupuestoResponse::desde)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return PresupuestoRespuestas.ejecutar(() -> PresupuestoResponse.desde(servicio.obtener(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody PresupuestoRequest request) {
        return PresupuestoRespuestas.ejecutar(() -> PresupuestoResponse.desde(
                servicio.modificar(id, request.turnoId(), solicitudes(request))));
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<?> anular(@PathVariable Long id) {
        return PresupuestoRespuestas.ejecutar(() -> PresupuestoResponse.desde(servicio.anular(id)));
    }

    private List<ItemPresupuestoSolicitud> solicitudes(PresupuestoRequest request) {
        if (request.items() == null) {
            return List.of();
        }
        return request.items().stream()
                .map(item -> new ItemPresupuestoSolicitud(item.tratamientoId(), item.cantidad(),
                        item.precio(), item.descripcion()))
                .toList();
    }
}
