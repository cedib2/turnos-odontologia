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
@RequestMapping("/feriados")
public class FeriadoController {

    private final CatalogoSchedulingService catalogo;

    public FeriadoController(CatalogoSchedulingService catalogo) {
        this.catalogo = catalogo;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody FeriadoRequest request) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.crearFeriado(
                request.fecha(), request.descripcion(), request.aplicaATodos()));
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return CatalogoRespuestas.ejecutar(catalogo::listarFeriados);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.obtenerFeriado(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody FeriadoRequest request) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.actualizarFeriado(
                id, request.fecha(), request.descripcion(), request.aplicaATodos()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarParcial(@PathVariable Long id, @RequestBody FeriadoRequest request) {
        return actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.eliminarFeriado(id));
    }
}
