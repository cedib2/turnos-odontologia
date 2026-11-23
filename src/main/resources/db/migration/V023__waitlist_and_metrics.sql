-- C-19 waitlist-auto-refill-metrics: lista de espera y métricas verificables.
-- Parte 3 de la reserva Flyway, después de V022 (C-16). No se editan V001-V022.
-- senia_id, turno_id, profesional_id y sillon_id son asociaciones sin FK física
-- (mismo patrón que V021/V022): la resolución referencial vive en la capa de dominio.
-- No se crean tablas de pago, caja, asiento, arqueo, notificación ni metas numéricas.

CREATE SEQUENCE lista_espera_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE metricas_ocupacion_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE lista_espera (
    id BIGINT NOT NULL DEFAULT nextval('lista_espera_id_seq'),
    candidato_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    sillon_id BIGINT NOT NULL,
    inicio_deseado TIMESTAMPTZ NOT NULL,
    fin_deseado TIMESTAMPTZ NOT NULL,
    deposito_requerido BOOLEAN NOT NULL DEFAULT FALSE,
    senia_id BIGINT,
    estado VARCHAR(30) NOT NULL DEFAULT 'activa',
    ofrecida_en TIMESTAMPTZ,
    turno_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_lista_espera PRIMARY KEY (id),
    CONSTRAINT ck_lista_espera_rango CHECK (fin_deseado > inicio_deseado),
    CONSTRAINT ck_lista_espera_estado CHECK (estado IN ('activa', 'ofrecida', 'adjudicada', 'expirada'))
);

-- Una misma candidatura no puede repetirse mientras sigue disputando el mismo rango.
CREATE UNIQUE INDEX uq_lista_espera_candidato_rango ON lista_espera
    (candidato_id, inicio_deseado, fin_deseado)
    WHERE activo = TRUE AND estado IN ('activa', 'ofrecida');

CREATE INDEX ix_lista_espera_elegibles ON lista_espera (estado, inicio_deseado)
    WHERE activo = TRUE;
CREATE INDEX ix_lista_espera_sillon ON lista_espera (sillon_id);
CREATE INDEX ix_lista_espera_senia ON lista_espera (senia_id);
CREATE INDEX ix_lista_espera_turno ON lista_espera (turno_id);

CREATE TABLE metricas_ocupacion (
    id BIGINT NOT NULL DEFAULT nextval('metricas_ocupacion_id_seq'),
    sillon_id BIGINT NOT NULL,
    periodo VARCHAR(7) NOT NULL,
    ocupacion NUMERIC(7,4),
    ausentismo NUMERIC(7,4),
    metodologia VARCHAR(2000) NOT NULL,
    calculada_en TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_metricas_ocupacion PRIMARY KEY (id),
    CONSTRAINT ck_metricas_periodo CHECK (periodo ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ck_metricas_metodologia CHECK (length(btrim(metodologia)) > 0),
    CONSTRAINT ck_metricas_ocupacion CHECK (ocupacion IS NULL OR (ocupacion >= 0 AND ocupacion <= 100)),
    CONSTRAINT ck_metricas_ausentismo CHECK (ausentismo IS NULL OR (ausentismo >= 0 AND ausentismo <= 100))
);

CREATE UNIQUE INDEX uq_metricas_sillon_periodo ON metricas_ocupacion (sillon_id, periodo)
    WHERE activo = TRUE;
CREATE INDEX ix_metricas_periodo ON metricas_ocupacion (periodo);
