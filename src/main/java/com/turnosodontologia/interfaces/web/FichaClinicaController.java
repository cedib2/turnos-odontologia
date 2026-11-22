package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.clinico.HistoriaClinica;
import com.turnosodontologia.application.clinico.HistoriaClinicaService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints indicativos de historia clínica (paths {@code (TBD)}), solo en dev/test como la
 * superficie de agenda de C-08. La autorización por rol la aplica el servicio.
 */
@RestController
@Profile({"dev", "test"})
@RequestMapping("/fichas")
public class FichaClinicaController {

    private final HistoriaClinicaService servicio;

    public FichaClinicaController(HistoriaClinicaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public Map<String, Long> abrir(@RequestBody AbrirFicha request) {
        long id = request.turnoId() != null && request.pacienteId() == null
                ? servicio.abrirDesdeTurno(request.turnoId())
                : servicio.abrirFicha(request.pacienteId(), request.turnoId());
        return Map.of("fichaId", id);
    }

    @GetMapping("/{id}")
    public HistoriaClinica consultar(@PathVariable long id) {
        return servicio.consultar(id);
    }

    @PostMapping("/{id}/anamnesis")
    public Map<String, Integer> anamnesis(@PathVariable long id, @RequestBody AnamnesisRequest request) {
        return Map.of("version", servicio.registrarAnamnesis(id, request.respuestas(), request.textoLibre()));
    }

    @PostMapping("/{id}/odontograma")
    public Map<String, Integer> odontograma(@PathVariable long id, @RequestBody OdontogramaRequest request) {
        return Map.of("version", servicio.registrarOdontograma(id, request.piezaFdi(), request.cara(), request.estado()));
    }

    @PostMapping("/{id}/evoluciones")
    public Map<String, Long> evolucion(@PathVariable long id, @RequestBody EvolucionRequest request) {
        return Map.of("evolucionId", servicio.registrarEvolucion(id, request.profesionalId(), request.turnoId(),
                request.texto(), request.reemplazaId()));
    }

    @PostMapping("/{id}/consentimientos")
    public Map<String, Integer> consentimiento(@PathVariable long id, @RequestBody ConsentimientoRequest request) {
        return Map.of("version", servicio.registrarConsentimiento(id, request.tipo(), request.texto()));
    }

    public record AbrirFicha(Long pacienteId, Long turnoId) {
    }

    public record AnamnesisRequest(Map<String, Object> respuestas, String textoLibre) {
    }

    public record OdontogramaRequest(int piezaFdi, String cara, String estado) {
    }

    public record EvolucionRequest(long profesionalId, Long turnoId, String texto, Long reemplazaId) {
    }

    public record ConsentimientoRequest(String tipo, String texto) {
    }
}
