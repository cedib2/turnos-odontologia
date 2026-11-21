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
@RequestMapping("/horarios")
public class HorarioController {

    private final CatalogoSchedulingService catalogo;

    public HorarioController(CatalogoSchedulingService catalogo) {
        this.catalogo = catalogo;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody HorarioRequest request) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.crearHorario(
                request.profesionalId(), request.diaSemana(), request.desde(), request.hasta()));
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return CatalogoRespuestas.ejecutar(catalogo::listarHorarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.obtenerHorario(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody HorarioRequest request) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.actualizarHorario(
                id, request.profesionalId(), request.diaSemana(), request.desde(), request.hasta()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarParcial(@PathVariable Long id, @RequestBody HorarioRequest request) {
        return actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return CatalogoRespuestas.ejecutar(() -> catalogo.eliminarHorario(id));
    }
}
