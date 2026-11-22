-- Registro de auditoría append-only (C-05).
-- V003 se aplica out-of-order detrás de V005/V006 en dev/test.
-- Límite de evidencia: el REVOKE alcanza al rol de aplicación; un superuser, el dueño de la tabla
-- o cualquier rol con privilegios DDL puede sortearlo (ALTER TABLE, DROP, flyway repair).
CREATE SEQUENCE auditoria_log_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE auditoria_log (
    id BIGINT NOT NULL DEFAULT nextval('auditoria_log_id_seq'),
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id BIGINT,
    actor_nombre VARCHAR(120) NOT NULL,
    actor_rol VARCHAR(40),
    actor_jti VARCHAR(120),
    modo VARCHAR(16) NOT NULL,
    accion VARCHAR(80) NOT NULL,
    entidad VARCHAR(60) NOT NULL,
    entidad_id BIGINT,
    ip INET,
    dispositivo VARCHAR(255),
    motivo VARCHAR(255),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(120) NOT NULL,
    hash_anterior CHAR(64) NOT NULL,
    hash_actual CHAR(64) NOT NULL,
    CONSTRAINT pk_auditoria_log PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_log_actor FOREIGN KEY (actor_id) REFERENCES usuarios (id),
    CONSTRAINT ck_auditoria_log_modo CHECK (modo IN ('REAL', 'SIMULADO', 'SYSTEM', 'PACIENTE')),
    CONSTRAINT ck_auditoria_log_hash_anterior CHECK (length(btrim(hash_anterior)) = 64),
    CONSTRAINT ck_auditoria_log_hash_actual CHECK (length(btrim(hash_actual)) = 64)
);

CREATE INDEX ix_auditoria_log_fecha ON auditoria_log (fecha);
CREATE INDEX ix_auditoria_log_entidad ON auditoria_log (entidad, entidad_id);
CREATE INDEX ix_auditoria_log_actor ON auditoria_log (actor_id);
CREATE INDEX ix_auditoria_log_correlacion ON auditoria_log (correlation_id);

CREATE OR REPLACE FUNCTION auditoria_log_rechaza_modificacion() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'auditoria_log es append-only: operación % rechazada', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_log_update
    BEFORE UPDATE ON auditoria_log
    FOR EACH ROW EXECUTE FUNCTION auditoria_log_rechaza_modificacion();

CREATE TRIGGER trg_auditoria_log_delete
    BEFORE DELETE ON auditoria_log
    FOR EACH ROW EXECUTE FUNCTION auditoria_log_rechaza_modificacion();

-- PostgreSQL no admite triggers TRUNCATE por fila: la restricción es de sentencia.
CREATE TRIGGER trg_auditoria_log_truncate
    BEFORE TRUNCATE ON auditoria_log
    FOR EACH STATEMENT EXECUTE FUNCTION auditoria_log_rechaza_modificacion();

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'turnos_app') THEN
        CREATE ROLE turnos_app NOLOGIN;
    END IF;
END;
$$;

GRANT INSERT, SELECT ON auditoria_log TO turnos_app;
GRANT USAGE ON SEQUENCE auditoria_log_id_seq TO turnos_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria_log FROM turnos_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria_log FROM PUBLIC;
