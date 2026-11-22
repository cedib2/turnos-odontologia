CREATE TABLE public_token_jti (
    jti VARCHAR(64) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    paciente_id VARCHAR(128),
    turno_id VARCHAR(64),
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT pk_public_token_jti PRIMARY KEY (jti),
    CONSTRAINT ck_public_token_jti_scope_no_vacio CHECK (length(btrim(scope)) > 0),
    CONSTRAINT ck_public_token_jti_vigencia CHECK (expires_at > issued_at)
);

CREATE UNIQUE INDEX uq_public_token_jti_hash ON public_token_jti (token_hash);

CREATE INDEX ix_public_token_jti_vigencia ON public_token_jti (expires_at)
    WHERE used_at IS NULL AND revoked_at IS NULL;

CREATE TABLE public_idempotencia (
    operacion VARCHAR(128) NOT NULL,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_public_idempotencia PRIMARY KEY (operacion, idempotency_key_hash),
    CONSTRAINT ck_public_idempotencia_operacion_no_vacia CHECK (length(btrim(operacion)) > 0)
);
