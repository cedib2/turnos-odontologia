CREATE SEQUENCE auth_sesiones_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE auth_tokens_refresh_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE auth_jti_revocados_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE auth_sesiones (
    id BIGINT NOT NULL DEFAULT nextval('auth_sesiones_id_seq'),
    usuario_id BIGINT NOT NULL,
    jti VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    ip_origen VARCHAR(64),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT pk_auth_sesiones PRIMARY KEY (id),
    CONSTRAINT uq_auth_sesiones_jti UNIQUE (jti),
    CONSTRAINT fk_auth_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_auth_sesiones_estado CHECK (estado IN ('ACTIVA', 'REVOCADA', 'EXPIRADA'))
);

CREATE TABLE auth_tokens_refresh (
    id BIGINT NOT NULL DEFAULT nextval('auth_tokens_refresh_id_seq'),
    sesion_id BIGINT NOT NULL,
    hash_token VARCHAR(128) NOT NULL,
    jti VARCHAR(64) NOT NULL,
    familia_id VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    rotated_from_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_auth_tokens_refresh PRIMARY KEY (id),
    CONSTRAINT uq_auth_tokens_refresh_hash UNIQUE (hash_token),
    CONSTRAINT uq_auth_tokens_refresh_jti UNIQUE (jti),
    CONSTRAINT fk_auth_tokens_refresh_sesion FOREIGN KEY (sesion_id) REFERENCES auth_sesiones (id),
    CONSTRAINT fk_auth_tokens_refresh_rotado FOREIGN KEY (rotated_from_id) REFERENCES auth_tokens_refresh (id),
    CONSTRAINT ck_auth_tokens_refresh_vigencia CHECK (expires_at > issued_at)
);

CREATE TABLE auth_jti_revocados (
    id BIGINT NOT NULL DEFAULT nextval('auth_jti_revocados_id_seq'),
    jti VARCHAR(64) NOT NULL,
    sesion_id BIGINT NOT NULL,
    motivo VARCHAR(40) NOT NULL DEFAULT 'LOGOUT',
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_auth_jti_revocados PRIMARY KEY (id),
    CONSTRAINT uq_auth_jti_revocados_jti UNIQUE (jti),
    CONSTRAINT fk_auth_jti_revocados_sesion FOREIGN KEY (sesion_id) REFERENCES auth_sesiones (id)
);

CREATE INDEX ix_auth_sesiones_usuario_estado ON auth_sesiones (usuario_id, estado);
CREATE INDEX ix_auth_sesiones_expires_at ON auth_sesiones (expires_at);
CREATE INDEX ix_auth_tokens_refresh_sesion_familia ON auth_tokens_refresh (sesion_id, familia_id);
CREATE INDEX ix_auth_tokens_refresh_familia_revocado ON auth_tokens_refresh (familia_id, revoked_at);
CREATE INDEX ix_auth_jti_revocados_revoked_at ON auth_jti_revocados (revoked_at);
