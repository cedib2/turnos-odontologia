CREATE SEQUENCE tratamientos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE horarios_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE feriados_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE tratamientos (
    id BIGINT NOT NULL DEFAULT nextval('tratamientos_id_seq'),
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    duracion_minutos INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_tratamientos PRIMARY KEY (id),
    CONSTRAINT uq_tratamientos_codigo UNIQUE (codigo),
    CONSTRAINT ck_tratamientos_duracion_positiva CHECK (duracion_minutos > 0)
);

CREATE TABLE horarios (
    id BIGINT NOT NULL DEFAULT nextval('horarios_id_seq'),
    profesional_id BIGINT,
    dia_semana INTEGER NOT NULL,
    desde TIME NOT NULL,
    hasta TIME NOT NULL,
    zona_horaria VARCHAR(64) NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_horarios PRIMARY KEY (id),
    CONSTRAINT fk_horarios_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales (id),
    CONSTRAINT ck_horarios_dia_semana CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horarios_orden CHECK (hasta > desde)
);

CREATE TABLE feriados (
    id BIGINT NOT NULL DEFAULT nextval('feriados_id_seq'),
    fecha DATE NOT NULL,
    descripcion VARCHAR(160) NOT NULL,
    aplica_a_todos BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_feriados PRIMARY KEY (id),
    CONSTRAINT uq_feriados_fecha UNIQUE (fecha)
);

CREATE INDEX ix_horarios_profesional_dia ON horarios (profesional_id, dia_semana) WHERE activo = true;
CREATE INDEX ix_feriados_fecha ON feriados (fecha) WHERE activo = true;
