package com.turnosodontologia.historiapaciente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.clinico.HistoriaClinica;
import com.turnosodontologia.application.clinico.HistoriaClinicaService;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso y copia de la propia historia clínica por el paciente (C-13; Ley 26.529 art. 14,
 * RN-CL-04, RN-SE-02).
 *
 * <ol>
 *   <li>Token {@code HISTORIA} de C-06 válido (firma, emisor, vigencia, {@code jti}).</li>
 *   <li>Identidad del titular verificada por {@link VerificadorIdentidadTitular} (mecanismo
 *       {@code (TBD)}; fuera de dev/test falla cerrado).</li>
 *   <li>Lectura de la historia vigente o entrega de una copia con hash y plazo registrado.</li>
 * </ol>
 *
 * <p>Cada acceso, exportación e intento denegado queda en el log append-only de auditoría con
 * actor {@code PACIENTE}. Solo se entregan registros existentes: sin diagnósticos
 * automatizados ni interpretación clínica.
 */
@Transactional
public class AccesoHistoriaPacienteService {

    private final PublicTokenService tokens;
    private final VerificadorIdentidadTitular identidad;
    private final HistoriaClinicaService historias;
    private final JdbcTemplate jdbc;
    private final AuditEventPublisher auditoria;
    private final Clock reloj;
    private final Duration plazoCopia;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public AccesoHistoriaPacienteService(PublicTokenService tokens, VerificadorIdentidadTitular identidad,
                                         HistoriaClinicaService historias, JdbcTemplate jdbc,
                                         AuditEventPublisher auditoria, Clock relojAccesoHistoria,
                                         @Value("${historia.copia.plazo-horas:48}") long plazoHoras) {
        if (plazoHoras < 1) {
            throw new IllegalArgumentException("plazo de copia inválido");
        }
        this.tokens = tokens;
        this.identidad = identidad;
        this.historias = historias;
        this.jdbc = jdbc;
        this.auditoria = auditoria;
        this.reloj = relojAccesoHistoria;
        this.plazoCopia = Duration.ofHours(plazoHoras);
    }

    public Consulta consultar(String token, String evidenciaIdentidad) {
        Autorizacion autorizacion = autorizar(token, evidenciaIdentidad, "consulta");
        if (!autorizacion.permitido()) {
            return new Consulta(false, autorizacion.motivo(), null);
        }
        Optional<HistoriaClinica> historia = historias.historiaDelTitular(autorizacion.pacienteId());
        auditar("historia.paciente.consultada", autorizacion, "", Map.of("conRegistros", historia.isPresent()));
        return new Consulta(true, "ok", historia.orElse(null));
    }

    public Copia exportar(String token, String evidenciaIdentidad) {
        Instant solicitada = reloj.instant();
        Autorizacion autorizacion = autorizar(token, evidenciaIdentidad, "exportacion");
        if (!autorizacion.permitido()) {
            return new Copia(false, autorizacion.motivo(), null, null, false);
        }
        HistoriaClinica historia = historias.historiaDelTitular(autorizacion.pacienteId()).orElse(null);
        byte[] documento = documento(autorizacion.pacienteId(), historia, solicitada);
        String hash = HexFormat.of().formatHex(sha256(documento));
        Instant vence = solicitada.plus(plazoCopia);
        Instant entregada = reloj.instant();
        Long copiaId = jdbc.queryForObject("INSERT INTO historia_copias (paciente_id, solicitada_at, vence_at, "
                        + "entregada_at, hash_documento, token_jti) VALUES (?, ?, ?, ?, ?, ?) RETURNING id", Long.class,
                autorizacion.pacienteId(), Timestamp.from(solicitada), Timestamp.from(vence),
                Timestamp.from(entregada), hash, autorizacion.jti());
        boolean dentroDelPlazo = !entregada.isAfter(vence);
        auditar("historia.paciente.exportada", autorizacion, "",
                Map.of("copiaId", copiaId, "dentroDelPlazo", dentroDelPlazo, "hashDocumento", hash));
        return new Copia(true, "ok", documento, hash, dentroDelPlazo);
    }

    private Autorizacion autorizar(String token, String evidencia, String operacion) {
        PublicTokenService.Validacion validacion = tokens.validarParaHistoria(token);
        PublicTokenClaims claims = validacion.claims();
        String jti = claims == null ? null : claims.jti();
        Long pacienteId = claims == null ? null : parsear(claims.pacienteId());
        if (!validacion.permitido()) {
            return denegar(new Autorizacion(false, validacion.motivo(), pacienteId, jti, operacion));
        }
        if (pacienteId == null || !existePaciente(pacienteId)) {
            return denegar(new Autorizacion(false, "token_no_valido", null, jti, operacion));
        }
        if (evidencia == null || evidencia.isBlank() || !identidad.verificar(pacienteId, evidencia)) {
            return denegar(new Autorizacion(false, "identidad_no_verificada", pacienteId, jti, operacion));
        }
        return new Autorizacion(true, "ok", pacienteId, jti, operacion);
    }

    private Autorizacion denegar(Autorizacion autorizacion) {
        auditar("historia.paciente.denegada", autorizacion, autorizacion.motivo(), Map.of());
        return autorizacion;
    }

    private void auditar(String tipo, Autorizacion autorizacion, String motivo, Map<String, Object> extra) {
        Map<String, Object> metadata = new LinkedHashMap<>(extra);
        metadata.put("operacion", autorizacion.operacion());
        if (autorizacion.jti() != null) {
            metadata.put("jti", autorizacion.jti());
        }
        auditoria.publish(AuditEvent.de(tipo, "paciente", autorizacion.pacienteId(),
                ActorSimulado.pacienteSinUsuario(), motivo, metadata, UUID.randomUUID().toString()));
    }

    private boolean existePaciente(long pacienteId) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM pacientes WHERE id = ?", Integer.class, pacienteId);
        return cantidad != null && cantidad > 0;
    }

    private byte[] documento(long pacienteId, HistoriaClinica historia, Instant emitido) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("pacienteId", pacienteId);
        contenido.put("emitido", emitido);
        contenido.put("base", "Ley 26.529 art. 14 — copia de la historia clínica del titular");
        contenido.put("registros", historia);
        try {
            return mapper.writeValueAsBytes(contenido);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Long parsear(String valor) {
        try {
            return valor == null ? null : Long.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static byte[] sha256(byte[] valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record Autorizacion(boolean permitido, String motivo, Long pacienteId, String jti, String operacion) {
    }

    public record Consulta(boolean permitido, String motivo, HistoriaClinica historia) {
    }

    public record Copia(boolean permitido, String motivo, byte[] documento, String hashDocumento,
                        boolean dentroDelPlazo) {
    }
}
