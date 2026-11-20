CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE SEQUENCE turnos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE bloqueos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE ocupaciones_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE turnos (
    id BIGINT NOT NULL DEFAULT nextval('turnos_id_seq'),
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    sillon_id BIGINT NOT NULL,
    tipo_tratamiento_id BIGINT NOT NULL,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL,
    duracion_minutos INTEGER NOT NULL,
    estado VARCHAR(40) NOT NULL,
    origen VARCHAR(40) NOT NULL,
    token_publico VARCHAR(120),
    es_sobreturno BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_override VARCHAR(255),
    rango TSTZRANGE GENERATED ALWAYS AS (tstzrange(inicio, fin, '[)')) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_turnos PRIMARY KEY (id),
    CONSTRAINT fk_turnos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_turnos_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales (id),
    CONSTRAINT fk_turnos_sillon FOREIGN KEY (sillon_id) REFERENCES sillones (id),
    CONSTRAINT fk_turnos_tratamiento FOREIGN KEY (tipo_tratamiento_id) REFERENCES tratamientos (id),
    CONSTRAINT ck_turnos_rango CHECK (fin > inicio),
    CONSTRAINT ck_turnos_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT ck_turnos_estado_no_vacio CHECK (length(btrim(estado)) > 0),
    CONSTRAINT ck_turnos_origen_no_vacio CHECK (length(btrim(origen)) > 0),
    CONSTRAINT ck_turnos_sobreturno_motivo CHECK (NOT es_sobreturno OR length(btrim(motivo_override)) > 0),
    CONSTRAINT ex_turnos_profesional EXCLUDE USING gist (profesional_id WITH =, rango WITH &&) WHERE (activo),
    CONSTRAINT ex_turnos_sillon EXCLUDE USING gist (sillon_id WITH =, rango WITH &&) WHERE (activo)
);

CREATE TABLE bloqueos (
    id BIGINT NOT NULL DEFAULT nextval('bloqueos_id_seq'),
    profesional_id BIGINT,
    sillon_id BIGINT,
    desde TIMESTAMPTZ NOT NULL,
    hasta TIMESTAMPTZ NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    rango TSTZRANGE GENERATED ALWAYS AS (tstzrange(desde, hasta, '[)')) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_bloqueos PRIMARY KEY (id),
    CONSTRAINT fk_bloqueos_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales (id),
    CONSTRAINT fk_bloqueos_sillon FOREIGN KEY (sillon_id) REFERENCES sillones (id),
    CONSTRAINT ck_bloqueos_rango CHECK (hasta > desde),
    CONSTRAINT ck_bloqueos_motivo_no_vacio CHECK (length(btrim(motivo)) > 0),
    CONSTRAINT ck_bloqueos_recurso CHECK (profesional_id IS NOT NULL OR sillon_id IS NOT NULL),
    CONSTRAINT ex_bloqueos_profesional EXCLUDE USING gist (profesional_id WITH =, rango WITH &&)
        WHERE (activo AND profesional_id IS NOT NULL),
    CONSTRAINT ex_bloqueos_sillon EXCLUDE USING gist (sillon_id WITH =, rango WITH &&)
        WHERE (activo AND sillon_id IS NOT NULL)
);

CREATE TABLE ocupaciones (
    id BIGINT NOT NULL DEFAULT nextval('ocupaciones_id_seq'),
    turno_id BIGINT,
    bloqueo_id BIGINT,
    profesional_id BIGINT,
    sillon_id BIGINT,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL,
    rango TSTZRANGE GENERATED ALWAYS AS (tstzrange(inicio, fin, '[)')) STORED,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_ocupaciones PRIMARY KEY (id),
    CONSTRAINT fk_ocupaciones_turno FOREIGN KEY (turno_id) REFERENCES turnos (id),
    CONSTRAINT fk_ocupaciones_bloqueo FOREIGN KEY (bloqueo_id) REFERENCES bloqueos (id),
    CONSTRAINT ck_ocupaciones_origen CHECK ((turno_id IS NOT NULL) <> (bloqueo_id IS NOT NULL)),
    CONSTRAINT ck_ocupaciones_recurso CHECK (profesional_id IS NOT NULL OR sillon_id IS NOT NULL),
    CONSTRAINT ex_ocupaciones_profesional EXCLUDE USING gist (profesional_id WITH =, rango WITH &&)
        WHERE (activo AND profesional_id IS NOT NULL),
    CONSTRAINT ex_ocupaciones_sillon EXCLUDE USING gist (sillon_id WITH =, rango WITH &&)
        WHERE (activo AND sillon_id IS NOT NULL)
);

CREATE INDEX ix_turnos_profesional_inicio_fin ON turnos (profesional_id, inicio, fin);
CREATE INDEX ix_turnos_sillon_inicio_fin ON turnos (sillon_id, inicio, fin);
CREATE INDEX ix_turnos_estado ON turnos (estado);
CREATE INDEX ix_turnos_token_publico ON turnos (token_publico);
CREATE INDEX ix_bloqueos_profesional_desde_hasta ON bloqueos (profesional_id, desde, hasta);
CREATE INDEX ix_bloqueos_sillon_desde_hasta ON bloqueos (sillon_id, desde, hasta);
