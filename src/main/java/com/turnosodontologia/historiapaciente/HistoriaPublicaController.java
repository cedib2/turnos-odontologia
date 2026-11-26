package com.turnosodontologia.historiapaciente;

import com.turnosodontologia.application.clinico.HistoriaClinica;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Acceso del titular a su propia historia clínica (C-13, Ley 26.529 art. 14) con el link que emite el staff
 * (token {@code HISTORIA} atado a un paciente) más la verificación de identidad del titular. El token solo
 * habilita la historia de su paciente: no hay parámetro de paciente, así que no se pueden pedir fichas ajenas.
 * Cada acceso y cada denegación quedan auditados por {@link AccesoHistoriaPacienteService}.
 *
 * <p>La evidencia de identidad viaja en el cuerpo (POST) y no en la URL, para que no quede en logs ni historial.
 */
@RestController
@RequestMapping("/publico/v1/historia")
public class HistoriaPublicaController {

    private final AccesoHistoriaPacienteService accesos;

    public HistoriaPublicaController(AccesoHistoriaPacienteService accesos) {
        this.accesos = accesos;
    }

    @PostMapping("/consulta")
    public ResponseEntity<?> consultar(@RequestParam String token, @RequestBody(required = false) Identidad identidad) {
        AccesoHistoriaPacienteService.Consulta consulta = accesos.consultar(token, evidencia(identidad));
        if (!consulta.permitido()) {
            return denegado(consulta.motivo());
        }
        return ResponseEntity.ok(new HistoriaDelTitular(consulta.historia() != null, consulta.historia()));
    }

    @PostMapping("/copia")
    public ResponseEntity<?> copia(@RequestParam String token, @RequestBody(required = false) Identidad identidad) {
        AccesoHistoriaPacienteService.Copia copia = accesos.exportar(token, evidencia(identidad));
        if (!copia.permitido()) {
            return denegado(copia.motivo());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("historia-clinica.json").build().toString())
                .header("X-Hash-Documento", copia.hashDocumento())
                .body(copia.documento());
    }

    private static String evidencia(Identidad identidad) {
        return identidad == null ? null : identidad.documento();
    }

    private static ResponseEntity<Map<String, String>> denegado(String motivo) {
        HttpStatus estado = "identidad_no_verificada".equals(motivo) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(estado).body(Map.of("error", motivo == null ? "token_no_valido" : motivo));
    }

    /** Evidencia de identidad del titular. En dev: su número de documento (mecanismo real {@code (TBD)}). */
    public record Identidad(String documento) {
    }

    /** {@code tieneFicha=false}: el paciente todavía no tiene registros clínicos. */
    public record HistoriaDelTitular(boolean tieneFicha, HistoriaClinica historia) {
    }
}
