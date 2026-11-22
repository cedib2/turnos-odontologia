package com.turnosodontologia.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class JdbcAuditEventPublisher implements AuditEventPublisher {

    private static final long LOCK_KEY = 7238745621098765432L;
    private static final String INSERT = """
            INSERT INTO auditoria_log
                (id, fecha, actor_id, actor_nombre, actor_rol, actor_jti, modo, accion,
                 entidad, entidad_id, ip, dispositivo, motivo, metadata, correlation_id,
                 hash_anterior, hash_actual)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS INET), ?, ?, CAST(? AS JSONB), ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AuditoriaCanonicalizer canonicalizer;
    private final AuditoriaCatalogo catalogo;
    private final AuditoriaMetadataValidator metadataValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcAuditEventPublisher(JdbcTemplate jdbcTemplate, AuditoriaCanonicalizer canonicalizer) {
        this(jdbcTemplate, canonicalizer, new AuditoriaCatalogo(), new AuditoriaMetadataValidator());
    }

    public JdbcAuditEventPublisher(JdbcTemplate jdbcTemplate, AuditoriaCanonicalizer canonicalizer,
                                   AuditoriaCatalogo catalogo) {
        this(jdbcTemplate, canonicalizer, catalogo, new AuditoriaMetadataValidator());
    }

    public JdbcAuditEventPublisher(JdbcTemplate jdbcTemplate, AuditoriaCanonicalizer canonicalizer,
                                   AuditoriaCatalogo catalogo,
                                   AuditoriaMetadataValidator metadataValidator) {
        this.jdbcTemplate = jdbcTemplate;
        this.canonicalizer = canonicalizer;
        this.catalogo = catalogo;
        this.metadataValidator = metadataValidator;
    }

    @Override
    public void publish(AuditEvent event) {
        catalogo.validar(event.tipo());
        metadataValidator.validar(event.metadata());
        if (event.actor() == null) {
            throw new IllegalArgumentException("actor de auditoría obligatorio");
        }
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + LOCK_KEY + ")");
        Long id = jdbcTemplate.queryForObject("SELECT nextval('auditoria_log_id_seq')", Long.class);
        java.util.List<String> hashAnteriores = jdbcTemplate.queryForList(
                "SELECT hash_actual FROM auditoria_log ORDER BY id DESC LIMIT 1", String.class);
        String hashAnterior = hashAnteriores.isEmpty()
                ? AuditoriaCanonicalizer.GENESIS
                : hashAnteriores.get(0);
        ActorSimulado actor = event.actor();
        Instant fecha = event.occurredAt().truncatedTo(ChronoUnit.MICROS);
        AuditoriaCanonicalizer.Fila fila = new AuditoriaCanonicalizer.Fila(
                id, fecha, actor.usuarioId(), actor.nombre(), actor.rol(), actor.jti(), actor.modo(),
                event.tipo(), event.entidad(), event.entidadId(), event.ip(), event.dispositivo(),
                event.motivo(), event.metadata(), event.correlationId(), hashAnterior);
        jdbcTemplate.update(INSERT,
                fila.id(), Timestamp.from(fila.fecha()), fila.actorId(), fila.actorNombre(), fila.actorRol(),
                fila.actorJti(), fila.modo(), fila.accion(), fila.entidad(), fila.entidadId(),
                fila.ip(), fila.dispositivo(), fila.motivo(), metadataJson(fila.metadata()),
                fila.correlationId(), fila.hashAnterior(), canonicalizer.hash(fila));
    }

    private String metadataJson(java.util.Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? java.util.Map.of() : metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metadata no serializable", e);
        }
    }
}
