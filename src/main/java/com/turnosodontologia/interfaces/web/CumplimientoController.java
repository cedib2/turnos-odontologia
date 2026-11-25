package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.cumplimiento.ExportacionService;
import com.turnosodontologia.application.cumplimiento.OperacionCumplimiento;
import com.turnosodontologia.application.cumplimiento.RespaldoService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Superficie indicativa dev/test de C-17 (exportación, respaldo y restore aislado). */
@RestController
@Profile({"dev", "test"})
@RequestMapping("/cumplimiento")
public class CumplimientoController {

    private final ExportacionService exportaciones;
    private final RespaldoService respaldos;

    public CumplimientoController(ExportacionService exportaciones, RespaldoService respaldos) {
        this.exportaciones = exportaciones;
        this.respaldos = respaldos;
    }

    public record ExportacionRequest(String alcance, Long pacienteId, String motivo) {
    }

    public record MotivoRequest(String motivo) {
    }

    @PostMapping("/exportaciones")
    public OperacionCumplimiento exportar(@RequestBody ExportacionRequest request) {
        return exportaciones.exportar(request.alcance(), request.pacienteId(), request.motivo());
    }

    @GetMapping(value = "/exportaciones/{id}/contenido", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> descargar(@PathVariable long id, @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=exportacion-" + id + ".json")
                .header("Cache-Control", "no-store")
                .body(exportaciones.descargar(id, motivo));
    }

    @GetMapping("/exportaciones/vencidas")
    public List<OperacionCumplimiento> vencidas() {
        return exportaciones.vencidas(Instant.now());
    }

    @PostMapping("/respaldos")
    public OperacionCumplimiento respaldar(@RequestBody(required = false) MotivoRequest request) {
        return respaldos.respaldar(request == null ? null : request.motivo());
    }

    @PostMapping("/respaldos/{id}/restore-aislado")
    public OperacionCumplimiento restaurar(@PathVariable long id,
                                           @RequestBody(required = false) MotivoRequest request) {
        return respaldos.restaurarEnAislado(id, request == null ? null : request.motivo());
    }
}
