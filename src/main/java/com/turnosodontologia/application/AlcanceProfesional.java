package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resuelve el ámbito de un permiso de staff según {@code rol_permiso} (seed V001): con ámbito {@code todos} el
 * actor ve todo; con {@code propio}, solo lo de su profesional, vinculado por {@code usuarios.profesional_id}
 * (V033). El actor simulado de dev/test toma su profesional de {@code turnos.simulado.profesional-id}.
 *
 * <p>Se consulta por SQL con {@code lower(ambito)} porque la columna guarda {@code propio}/{@code todos} en
 * minúsculas (CHECK de V001) y el enum JPA {@code Ambito} está en mayúsculas.
 */
@Service
public class AlcanceProfesional {

    /** Alcance efectivo: {@code todos}, o restringido a un profesional. */
    public record Alcance(boolean todos, Long profesionalId) {

        public static Alcance completo() {
            return new Alcance(true, null);
        }
    }

    private final JdbcTemplate jdbc;
    private final Long profesionalSimulado;

    public AlcanceProfesional(JdbcTemplate jdbc,
                              @Value("${turnos.simulado.profesional-id:#{null}}") Long profesionalSimulado) {
        this.jdbc = jdbc;
        this.profesionalSimulado = profesionalSimulado;
    }

    /**
     * Alcance del actor para una acción sobre los recursos dados. Falla con {@link AccesoFueraDeAlcanceException}
     * si solo tiene ámbito {@code propio} y no está vinculado a un profesional, o si no tiene el permiso.
     */
    public Alcance de(ActorSimulado actor, Set<String> recursos, String accion) {
        if (actor == null || actor.rol() == null) {
            throw new AccesoFueraDeAlcanceException("actor_sin_rol");
        }
        if ("SYSTEM".equalsIgnoreCase(actor.modo())) {
            return Alcance.completo();
        }
        String marcadores = String.join(", ", recursos.stream().map(r -> "?").toList());
        Object[] parametros = new Object[recursos.size() + 2];
        parametros[0] = actor.rol().toLowerCase(Locale.ROOT);
        int i = 1;
        for (String recurso : recursos) {
            parametros[i++] = recurso;
        }
        parametros[i] = accion;
        List<String> ambitos = jdbc.queryForList("""
                SELECT DISTINCT lower(p.ambito)
                FROM rol_permiso rp
                JOIN roles r ON r.id = rp.rol_id
                JOIN permisos p ON p.id = rp.permiso_id
                WHERE lower(r.codigo) = ? AND p.recurso IN (%s) AND p.accion = ?
                """.formatted(marcadores), String.class, parametros);
        if (ambitos.contains("todos")) {
            return Alcance.completo();
        }
        if (ambitos.contains("propio")) {
            Long profesional = profesionalDe(actor);
            if (profesional == null) {
                throw new AccesoFueraDeAlcanceException("usuario_sin_profesional_vinculado");
            }
            return new Alcance(false, profesional);
        }
        throw new AccesoFueraDeAlcanceException("sin_permiso");
    }

    /** Profesional vinculado al actor: el de su usuario (REAL) o el configurado para el actor simulado. */
    public Long profesionalDe(ActorSimulado actor) {
        if (actor.usuarioId() == null) {
            return "SIMULADO".equalsIgnoreCase(actor.modo()) ? profesionalSimulado : null;
        }
        return jdbc.queryForList("SELECT profesional_id FROM usuarios WHERE id = ?", Long.class, actor.usuarioId())
                .stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }
}
