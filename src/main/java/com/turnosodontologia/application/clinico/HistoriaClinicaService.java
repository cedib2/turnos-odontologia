package com.turnosodontologia.application.clinico;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ficha clínica, anamnesis, odontograma FDI, evoluciones y consentimientos (C-11).
 *
 * <ul>
 *   <li>Una ficha por paciente; se puede abrir desde un turno del mismo paciente.</li>
 *   <li>Nomenclatura FDI obligatoria con piezas temporales: la pieza debe existir en
 *       {@code piezas_fdi} (RN-CL-01).</li>
 *   <li>Nunca se sobrescribe: cada registro es una versión nueva y la anterior se conserva
 *       (RN-CL-02); la base lo refuerza con triggers append-only.</li>
 *   <li>Cada escritura incrementa {@code historia_version}, exige permiso por rol y se audita
 *       sin contenido clínico en la metadata.</li>
 * </ul>
 */
@Service
@Transactional(noRollbackFor = AccesoClinicoDenegadoException.class)
public class HistoriaClinicaService {

    static final String CREAR = "crear";
    static final String LEER = "leer";

    private final JdbcTemplate jdbc;
    private final CurrentActorProvider actores;
    private final AutorizacionClinica autorizacion;
    private final AuditEventPublisher auditoria;
    private final ObjectMapper mapper = new ObjectMapper();

    public HistoriaClinicaService(JdbcTemplate jdbc, CurrentActorProvider actores,
                                  AutorizacionClinica autorizacion, AuditEventPublisher auditoria) {
        this.jdbc = jdbc;
        this.actores = actores;
        this.autorizacion = autorizacion;
        this.auditoria = auditoria;
    }

    public long abrirFicha(long pacienteId, Long turnoId) {
        ActorSimulado actor = actores.current();
        autorizacion.exigir(actor, CREAR, null);
        if (turnoId != null) {
            Long pacienteDelTurno = jdbc.query("SELECT paciente_id FROM turnos WHERE id = ?",
                    (rs, i) -> rs.getLong(1), turnoId).stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Turno inexistente: " + turnoId));
            if (pacienteDelTurno != pacienteId) {
                throw new IllegalArgumentException("El turno no pertenece al paciente");
            }
        }
        List<Long> existente = jdbc.queryForList(
                "SELECT id FROM fichas_clinicas WHERE paciente_id = ?", Long.class, pacienteId);
        if (!existente.isEmpty()) {
            return existente.get(0);
        }
        List<Long> creada = jdbc.queryForList("INSERT INTO fichas_clinicas (paciente_id, turno_apertura_id) "
                + "VALUES (?, ?) ON CONFLICT (paciente_id) DO NOTHING RETURNING id", Long.class, pacienteId, turnoId);
        if (creada.isEmpty()) {
            return jdbc.queryForObject("SELECT id FROM fichas_clinicas WHERE paciente_id = ?", Long.class, pacienteId);
        }
        Long id = creada.get(0);
        auditar("clinico.ficha.abierta", id, actor, Map.of("pacienteId", pacienteId,
                "desdeTurno", turnoId != null));
        return id;
    }

    public long abrirDesdeTurno(long turnoId) {
        Long paciente = jdbc.query("SELECT paciente_id FROM turnos WHERE id = ?", (rs, i) -> rs.getLong(1), turnoId)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Turno inexistente: " + turnoId));
        return abrirFicha(paciente, turnoId);
    }

    public int registrarAnamnesis(long fichaId, Map<String, Object> respuestas, String textoLibre) {
        ActorSimulado actor = actores.current();
        autorizacion.exigir(actor, CREAR, fichaId);
        bloquearFicha(fichaId);
        int version = siguiente("SELECT MAX(version) FROM anamnesis WHERE ficha_id = ?", fichaId);
        jdbc.update("INSERT INTO anamnesis (ficha_id, version, respuestas, texto_libre, autor_id, autor_nombre, "
                        + "autor_modo) VALUES (?, ?, CAST(? AS JSONB), ?, ?, ?, ?)",
                fichaId, version, json(respuestas), textoLibre, actor.usuarioId(), actor.nombre(), actor.modo());
        cerrarCambio(fichaId, "clinico.anamnesis.registrada", actor, Map.of("version", version));
        return version;
    }

    public int registrarOdontograma(long fichaId, int piezaFdi, String cara, String estado) {
        ActorSimulado actor = actores.current();
        autorizacion.exigir(actor, CREAR, fichaId);
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("estado obligatorio");
        }
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM piezas_fdi WHERE codigo = ?", Integer.class, piezaFdi);
        if (existe == null || existe == 0) {
            throw new IllegalArgumentException("Pieza fuera de la nomenclatura FDI: " + piezaFdi);
        }
        String caraNormalizada = cara == null || cara.isBlank() ? null : cara.trim();
        bloquearFicha(fichaId);
        int version = siguiente("SELECT MAX(version) FROM odontograma_registros WHERE ficha_id = ? "
                + "AND pieza_fdi = ? AND COALESCE(cara, '') = COALESCE(?, '')", fichaId, piezaFdi, caraNormalizada);
        jdbc.update("INSERT INTO odontograma_registros (ficha_id, pieza_fdi, cara, estado, version, autor_id, "
                        + "autor_nombre, autor_modo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                fichaId, piezaFdi, caraNormalizada, estado.trim(), version, actor.usuarioId(), actor.nombre(),
                actor.modo());
        cerrarCambio(fichaId, "clinico.odontograma.registrado", actor,
                Map.of("piezaFdi", piezaFdi, "version", version));
        return version;
    }

    /** Registra una evolución; con {@code reemplazaId} crea la versión siguiente de esa evolución. */
    public long registrarEvolucion(long fichaId, long profesionalId, Long turnoId, String texto, Long reemplazaId) {
        ActorSimulado actor = actores.current();
        autorizacion.exigir(actor, CREAR, fichaId);
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("texto obligatorio");
        }
        bloquearFicha(fichaId);
        int version = 1;
        if (reemplazaId != null) {
            Map<String, Object> anterior = jdbc.queryForList("SELECT ficha_id, version FROM evoluciones WHERE id = ?",
                    reemplazaId).stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Evolución inexistente: " + reemplazaId));
            if (((Number) anterior.get("ficha_id")).longValue() != fichaId) {
                throw new IllegalArgumentException("La evolución pertenece a otra ficha");
            }
            Integer reemplazos = jdbc.queryForObject("SELECT COUNT(*) FROM evoluciones WHERE reemplaza_id = ?",
                    Integer.class, reemplazaId);
            if (reemplazos != null && reemplazos > 0) {
                throw new IllegalStateException("La evolución ya fue reemplazada por una versión posterior");
            }
            version = ((Number) anterior.get("version")).intValue() + 1;
        }
        Long id = jdbc.queryForObject("INSERT INTO evoluciones (ficha_id, profesional_id, turno_id, version, "
                        + "reemplaza_id, texto, autor_id, autor_nombre, autor_modo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "RETURNING id", Long.class,
                fichaId, profesionalId, turnoId, version, reemplazaId, texto, actor.usuarioId(), actor.nombre(),
                actor.modo());
        Map<String, Object> metadata = new HashMap<>(Map.of("evolucionId", id, "version", version));
        if (reemplazaId != null) {
            metadata.put("reemplazaId", reemplazaId);
        }
        cerrarCambio(fichaId, "clinico.evolucion.registrada", actor, metadata);
        return id;
    }

    public int registrarConsentimiento(long fichaId, String tipo, String texto) {
        ActorSimulado actor = actores.current();
        autorizacion.exigir(actor, CREAR, fichaId);
        if (tipo == null || tipo.isBlank() || texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("tipo y texto obligatorios");
        }
        bloquearFicha(fichaId);
        int version = siguiente("SELECT MAX(version) FROM consentimientos WHERE ficha_id = ? AND tipo = ?",
                fichaId, tipo.trim());
        jdbc.update("INSERT INTO consentimientos (ficha_id, tipo, texto, version, autor_id, autor_nombre, autor_modo) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                fichaId, tipo.trim(), texto, version, actor.usuarioId(), actor.nombre(), actor.modo());
        cerrarCambio(fichaId, "clinico.consentimiento.registrado", actor, Map.of("version", version));
        return version;
    }

    @Transactional(readOnly = true, noRollbackFor = AccesoClinicoDenegadoException.class)
    public HistoriaClinica consultar(long fichaId) {
        ActorSimulado actor = actores.current();
        autorizacion.exigir(actor, LEER, fichaId);
        return leer(fichaId);
    }

    /** Lectura sin control de rol, para servicios que ya autorizaron por otra vía (p. ej. C-13). */
    HistoriaClinica leer(long fichaId) {
        Map<String, Object> ficha = jdbc.queryForList(
                "SELECT paciente_id, historia_version FROM fichas_clinicas WHERE id = ?", fichaId)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Ficha inexistente: " + fichaId));

        HistoriaClinica.Anamnesis anamnesis = jdbc.query("SELECT * FROM anamnesis WHERE ficha_id = ? "
                        + "ORDER BY version DESC LIMIT 1", (rs, i) -> new HistoriaClinica.Anamnesis(
                        rs.getLong("id"), rs.getInt("version"), mapa(rs.getString("respuestas")),
                        rs.getString("texto_libre"), rs.getTimestamp("fecha").toInstant(),
                        rs.getString("autor_nombre")), fichaId)
                .stream().findFirst().orElse(null);

        List<HistoriaClinica.RegistroOdontograma> odontograma = jdbc.query(
                "SELECT DISTINCT ON (o.pieza_fdi, COALESCE(o.cara, '')) o.*, p.temporal FROM odontograma_registros o "
                        + "JOIN piezas_fdi p ON p.codigo = o.pieza_fdi WHERE o.ficha_id = ? "
                        + "ORDER BY o.pieza_fdi, COALESCE(o.cara, ''), o.version DESC",
                (rs, i) -> new HistoriaClinica.RegistroOdontograma(rs.getLong("id"), rs.getInt("pieza_fdi"),
                        rs.getBoolean("temporal"), rs.getString("cara"), rs.getString("estado"),
                        rs.getInt("version"), rs.getTimestamp("fecha").toInstant(), rs.getString("autor_nombre")),
                fichaId);

        List<HistoriaClinica.Evolucion> evoluciones = jdbc.query(
                "SELECT e.* FROM evoluciones e WHERE e.ficha_id = ? AND NOT EXISTS "
                        + "(SELECT 1 FROM evoluciones r WHERE r.reemplaza_id = e.id) ORDER BY e.fecha, e.id",
                (rs, i) -> new HistoriaClinica.Evolucion(rs.getLong("id"), rs.getLong("profesional_id"),
                        rs.getInt("version"), (Long) rs.getObject("reemplaza_id"), rs.getString("texto"),
                        rs.getTimestamp("fecha").toInstant(), rs.getString("autor_nombre")), fichaId);

        List<HistoriaClinica.Consentimiento> consentimientos = jdbc.query(
                "SELECT DISTINCT ON (tipo) * FROM consentimientos WHERE ficha_id = ? ORDER BY tipo, version DESC",
                (rs, i) -> new HistoriaClinica.Consentimiento(rs.getLong("id"), rs.getString("tipo"),
                        rs.getInt("version"), rs.getString("texto"), rs.getTimestamp("fecha").toInstant(),
                        rs.getString("autor_nombre")), fichaId);

        return new HistoriaClinica(fichaId, ((Number) ficha.get("paciente_id")).longValue(),
                ((Number) ficha.get("historia_version")).intValue(), anamnesis, odontograma, evoluciones,
                consentimientos);
    }

    private void bloquearFicha(long fichaId) {
        List<Long> filas = jdbc.queryForList("SELECT id FROM fichas_clinicas WHERE id = ? FOR UPDATE",
                Long.class, fichaId);
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("Ficha inexistente: " + fichaId);
        }
    }

    private int siguiente(String sql, Object... args) {
        Integer max = jdbc.queryForObject(sql, Integer.class, args);
        return max == null ? 1 : max + 1;
    }

    private void cerrarCambio(long fichaId, String tipo, ActorSimulado actor, Map<String, Object> metadata) {
        Integer version = jdbc.queryForObject("UPDATE fichas_clinicas SET historia_version = historia_version + 1, "
                + "updated_at = now() WHERE id = ? RETURNING historia_version", Integer.class, fichaId);
        Map<String, Object> completa = new HashMap<>(metadata);
        completa.put("historiaVersion", version);
        auditar(tipo, fichaId, actor, completa);
    }

    private void auditar(String tipo, Long fichaId, ActorSimulado actor, Map<String, Object> metadata) {
        auditoria.publish(AuditEvent.de(tipo, "ficha", fichaId, actor, "", metadata, UUID.randomUUID().toString()));
    }

    private String json(Map<String, Object> respuestas) {
        try {
            return mapper.writeValueAsString(respuestas == null ? Map.of() : respuestas);
        } catch (Exception e) {
            throw new IllegalArgumentException("respuestas no serializables", e);
        }
    }

    private Map<String, Object> mapa(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
