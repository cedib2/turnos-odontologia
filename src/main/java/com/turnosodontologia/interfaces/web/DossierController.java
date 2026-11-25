package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.cumplimiento.Dossier;
import com.turnosodontologia.application.cumplimiento.DossierIncompletoException;
import com.turnosodontologia.application.cumplimiento.DossierService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Superficie indicativa dev/test de C-18 (dossier de cumplimiento demostrable). */
@RestController
@Profile({"dev", "test"})
@RequestMapping("/cumplimiento/dossier")
public class DossierController {

    private final DossierService dossier;

    public DossierController(DossierService dossier) {
        this.dossier = dossier;
    }

    public record EvidenciaRequest(String referencia, String motivo) {
    }

    public record SubprocesadorRequest(String nombre, String servicio, String ubicacionDatos,
                                       String clausulasAaipReferencia, String dpaReferencia) {
    }

    @GetMapping
    public ResponseEntity<Dossier> descargar(@RequestParam(required = false) String motivo) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=dossier-cumplimiento.json")
                .header("Cache-Control", "no-store")
                .body(dossier.descargar(motivo));
    }

    @PostMapping("/publicacion")
    public Dossier publicar(@RequestParam(required = false) String motivo) {
        return dossier.publicar(motivo);
    }

    @PutMapping("/items/{codigo}/evidencia")
    public Dossier.Item registrarEvidencia(@PathVariable String codigo, @RequestBody EvidenciaRequest request) {
        return dossier.registrarEvidencia(codigo, request.referencia(), request.motivo());
    }

    @PostMapping("/subprocesadores")
    public Dossier.Subprocesador registrarSubprocesador(@RequestBody SubprocesadorRequest request) {
        return dossier.registrarSubprocesador(request.nombre(), request.servicio(), request.ubicacionDatos(),
                request.clausulasAaipReferencia(), request.dpaReferencia());
    }

    @ExceptionHandler(DossierIncompletoException.class)
    public ResponseEntity<Map<String, Object>> incompleto(DossierIncompletoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "dossier_incompleto", "pendientes", e.pendientes()));
    }
}
