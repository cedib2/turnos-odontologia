package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.CatalogoSchedulingService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/tratamientos")
public class TratamientoController {

    private final CatalogoSchedulingService catalogo;

    public TratamientoController(CatalogoSchedulingService catalogo) {
        this.catalogo = catalogo;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TratamientoRequest request) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.crearTratamiento(
                request.codigo(), request.nombre(), request.duracionMinutos()));
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return CatalogoRespuestas.ejecutar(catalogo::listarTratamientos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.obtenerTratamiento(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody TratamientoRequest request) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.actualizarTratamiento(
                id, request.codigo(), request.nombre(), request.duracionMinutos()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarParcial(@PathVariable Long id, @RequestBody TratamientoRequest request) {
        return actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.eliminarTratamiento(id));
    }
}
