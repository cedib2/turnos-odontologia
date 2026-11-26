package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.PacienteService;
import com.turnosodontologia.application.UsuarioService;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import com.turnosodontologia.publictoken.PublicTokenService;
import com.turnosodontologia.publictoken.PublicTokenSigner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Superficie indicativa dev/test para el frontend: listados y alta de pacientes, listados de profesionales y
 * sillones, vínculo entre usuario y profesional, y
 * emisión del link de reserva del paciente (token {@code RESERVAR} de C-06, vigencia de 1 hora) para un
 * profesional, un sillón y un tratamiento.
 * Devuelve DTOs propios, no entidades JPA.
 */
@RestController
@Profile({"dev", "test"})
public class DirectorioController {

    static final String RUTA_RESERVA = "/reservar.html";

    private final PacienteRepository pacientes;
    private final ProfesionalRepository profesionales;
    private final SillonRepository sillones;
    private final PublicTokenService tokens;
    private final PublicTokenSigner firmador;
    private final Clock reloj;
    private final PacienteService altaPacientes;
    private final UsuarioService usuarios;

    public DirectorioController(PacienteRepository pacientes, ProfesionalRepository profesionales,
                                SillonRepository sillones, PublicTokenService tokens, PublicTokenSigner firmador,
                                @Qualifier("publicClock") Clock reloj, PacienteService altaPacientes,
                                UsuarioService usuarios) {
        this.altaPacientes = altaPacientes;
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.tokens = tokens;
        this.firmador = firmador;
        this.reloj = reloj;
    }

    public record PacienteVista(Long id, String documento, String nombre, String apellido, String telefono) {
        static PacienteVista de(Paciente p) {
            return new PacienteVista(p.getId(), p.getDocumentoNumero(), p.getNombre(), p.getApellido(),
                    p.getTelefono());
        }
    }

    public record ProfesionalVista(Long id, String matricula, String nombre, String apellido, String especialidad) {
        static ProfesionalVista de(Profesional p) {
            return new ProfesionalVista(p.getId(), p.getMatricula(), p.getNombre(), p.getApellido(),
                    p.getEspecialidad());
        }
    }

    public record SillonVista(Long id, String numero, Long consultorioId) {
        static SillonVista de(Sillon s) {
            return new SillonVista(s.getId(), s.getNumero(), s.getConsultorio().getId());
        }
    }

    public record AltaPacienteRequest(DocumentoTipo documentoTipo, String documento, String nombre, String apellido,
                                      String telefono) {
    }

    public record VinculoUsuarioRequest(Long usuarioId) {
    }

    public record EnlaceReservaRequest(Long profesionalId, String codigoTratamiento, Long sillonId) {
    }

    public record EnlaceReservaResponse(String token, String ruta, Instant venceAt) {
    }

    @GetMapping("/pacientes")
    public List<PacienteVista> pacientes() {
        return pacientes.findAll().stream()
                .sorted(Comparator.comparing(Paciente::getApellido, Comparator.nullsLast(String::compareTo)))
                .map(PacienteVista::de).toList();
    }

    @GetMapping("/profesionales")
    public List<ProfesionalVista> profesionales() {
        return profesionales.findAll().stream()
                .sorted(Comparator.comparing(Profesional::getApellido, Comparator.nullsLast(String::compareTo)))
                .map(ProfesionalVista::de).toList();
    }

    @GetMapping("/sillones")
    public List<SillonVista> sillones() {
        return sillones.findAll().stream()
                .sorted(Comparator.comparing(Sillon::getNumero, Comparator.nullsLast(String::compareTo)))
                .map(SillonVista::de).toList();
    }

    /** Alta de paciente por recepción. Un documento repetido responde 409. */
    @PostMapping("/pacientes")
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteVista altaPaciente(@RequestBody AltaPacienteRequest request) {
        if (request == null || request.documentoTipo() == null || vacio(request.documento())
                || vacio(request.nombre()) || vacio(request.apellido()) || vacio(request.telefono())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "documento_tipo_documento_nombre_apellido_y_telefono_requeridos");
        }
        return PacienteVista.de(altaPacientes.registrar(request.documentoTipo(), request.documento().trim(),
                request.nombre().trim(), request.apellido().trim(), request.telefono().trim()));
    }

    /**
     * Vincula el usuario de staff que representa a un profesional (V033): define el ámbito {@code propio} de su
     * agenda y de las fichas clínicas. Con {@code usuarioId} nulo se desvincula.
     */
    @PutMapping("/profesionales/{id}/usuario")
    public ProfesionalVista vincularUsuario(@PathVariable Long id, @RequestBody VinculoUsuarioRequest request) {
        Profesional profesional = profesionales.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profesional_inexistente"));
        if (request == null || request.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario_requerido");
        }
        usuarios.vincularProfesional(request.usuarioId(), id);
        return ProfesionalVista.de(profesional);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Emite el link de reserva (token {@code RESERVAR}) para un paciente, un profesional y un tratamiento. */
    @PostMapping("/pacientes/{id}/enlace-reserva")
    public EnlaceReservaResponse enlaceReserva(@PathVariable Long id, @RequestBody EnlaceReservaRequest request) {
        if (!pacientes.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "paciente_inexistente");
        }
        // La reserva pública (C-09) exige sillón al crear el turno: un link sin sillón mostraría
        // horarios que después no se podrían reservar.
        if (request == null || request.profesionalId() == null || request.sillonId() == null
                || request.codigoTratamiento() == null || request.codigoTratamiento().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profesional_sillon_y_tratamiento_requeridos");
        }
        if (!sillones.existsById(request.sillonId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "sillon_inexistente");
        }
        if (!profesionales.existsById(request.profesionalId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "profesional_inexistente");
        }
        long ahora = reloj.instant().getEpochSecond();
        PublicTokenScope scope = PublicTokenScope.RESERVAR;
        String token = tokens.emitir(new PublicTokenClaims("jti-" + UUID.randomUUID(), firmador.emisor(),
                "paciente-" + id, String.valueOf(id), scope, ahora, ahora + scope.vigenciaSegundos(), null, false));
        UriComponentsBuilder ruta = UriComponentsBuilder.fromPath(RUTA_RESERVA)
                .queryParam("token", token)
                .queryParam("paciente", id)
                .queryParam("profesional", request.profesionalId())
                .queryParam("tratamiento", request.codigoTratamiento())
                .queryParam("sillon", request.sillonId());
        return new EnlaceReservaResponse(token, ruta.encode().build().toUriString(),
                Instant.ofEpochSecond(ahora + scope.vigenciaSegundos()));
    }
}
