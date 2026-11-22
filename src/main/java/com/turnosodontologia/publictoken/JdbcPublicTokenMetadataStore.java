package com.turnosodontologia.publictoken;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

public class JdbcPublicTokenMetadataStore implements PublicTokenMetadataStore {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    public JdbcPublicTokenMetadataStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.transacciones = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public void registrar(PublicTokenClaims claims, String hash) {
        jdbc.update("INSERT INTO public_token_jti "
                        + "(jti, token_hash, scope, paciente_id, turno_id, issued_at, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                claims.jti(), hash, claims.scope().name(), claims.pacienteId(), claims.turnoId(),
                Timestamp.from(Instant.ofEpochSecond(claims.iat())),
                Timestamp.from(Instant.ofEpochSecond(claims.exp())));
    }

    @Override
    public boolean estaDisponible(String jti) {
        Boolean disponible = jdbc.queryForObject(
                "SELECT used_at IS NULL AND revoked_at IS NULL FROM public_token_jti WHERE jti = ?",
                Boolean.class, jti);
        return Boolean.TRUE.equals(disponible);
    }

    @Override
    public boolean marcarUsado(String jti) {
        return jdbc.update("UPDATE public_token_jti SET used_at = now() "
                + "WHERE jti = ? AND used_at IS NULL AND revoked_at IS NULL", jti) > 0;
    }

    @Override
    public boolean revocar(String jti) {
        return jdbc.update("UPDATE public_token_jti SET revoked_at = now() "
                + "WHERE jti = ? AND revoked_at IS NULL", jti) > 0;
    }

    @Override
    public void rotar(String anterior, PublicTokenClaims nuevo, String hashNuevo) {
        transacciones.executeWithoutResult(estado -> {
            Integer gestionables = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM public_token_jti "
                            + "WHERE jti = ? AND scope = ? AND used_at IS NULL AND revoked_at IS NULL",
                    Integer.class, anterior, PublicTokenScope.GESTIONAR.name());
            if (gestionables == null || gestionables == 0) {
                throw new IllegalArgumentException("rotacion_no_permitida");
            }
            jdbc.update("UPDATE public_token_jti SET used_at = now() WHERE jti = ?", anterior);
            registrar(nuevo, hashNuevo);
        });
    }
}
