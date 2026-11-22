-- C-11 clinical-record-odontogram (Parte 2).
-- Ficha clínica única por paciente, anamnesis mínima versionada (C-01 C4), odontograma FDI con
-- piezas permanentes y temporales (RN-CL-01), evoluciones y consentimientos.
-- Historia versionada sin sobrescritura (RN-CL-02): los registros clínicos son append-only;
-- un cambio es una versión nueva. Borrado físico (TBD, RN-CL-05): hoy prohibido por trigger.

CREATE TABLE piezas_fdi (
    codigo SMALLINT NOT NULL,
    cuadrante SMALLINT NOT NULL,
    posicion SMALLINT NOT NULL,
    temporal BOOLEAN NOT NULL,
    CONSTRAINT pk_piezas_fdi PRIMARY KEY (codigo),
    CONSTRAINT ck_piezas_fdi_codigo CHECK (codigo = cuadrante * 10 + posicion),
    CONSTRAINT ck_piezas_fdi_cuadrante CHECK (cuadrante BETWEEN 1 AND 8),
    CONSTRAINT ck_piezas_fdi_temporal CHECK (temporal = (cuadrante >= 5)),
    CONSTRAINT ck_piezas_fdi_posicion CHECK (
        (cuadrante <= 4 AND posicion BETWEEN 1 AND 8) OR (cuadrante >= 5 AND posicion BETWEEN 1 AND 5))
);

-- Seed FDI: 32 piezas permanentes (cuadrantes 1-4) y 20 temporales (cuadrantes 5-8).
INSERT INTO piezas_fdi (codigo, cuadrante, posicion, temporal)
SELECT c * 10 + p, c, p, c >= 5
FROM generate_series(1, 8) AS c
CROSS JOIN generate_series(1, 8) AS p
WHERE (c <= 4) OR (p <= 5);

CREATE SEQUENCE fichas_clinicas_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE anamnesis_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE odontograma_registros_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE evoluciones_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE consentimientos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE fichas_clinicas (
    id BIGINT NOT NULL DEFAULT nextval('fichas_clinicas_id_seq'),
    paciente_id BIGINT NOT NULL,
    turno_apertura_id BIGINT,
    historia_version INTEGER NOT NULL DEFAULT 0,
    fecha_apertura TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_fichas_clinicas PRIMARY KEY (id),
    CONSTRAINT uq_fichas_clinicas_paciente UNIQUE (paciente_id),
    CONSTRAINT fk_fichas_clinicas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_fichas_clinicas_turno FOREIGN KEY (turno_apertura_id) REFERENCES turnos (id),
    CONSTRAINT ck_fichas_clinicas_version CHECK (historia_version >= 0)
);

CREATE TABLE anamnesis (
    id BIGINT NOT NULL DEFAULT nextval('anamnesis_id_seq'),
    ficha_id BIGINT NOT NULL,
    version INTEGER NOT NULL,
    respuestas JSONB NOT NULL DEFAULT '{}'::jsonb,
    texto_libre TEXT,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    autor_id BIGINT,
    autor_nombre VARCHAR(120) NOT NULL,
    autor_modo VARCHAR(16) NOT NULL,
    CONSTRAINT pk_anamnesis PRIMARY KEY (id),
    CONSTRAINT fk_anamnesis_ficha FOREIGN KEY (ficha_id) REFERENCES fichas_clinicas (id),
    CONSTRAINT uq_anamnesis_version UNIQUE (ficha_id, version),
    CONSTRAINT ck_anamnesis_version CHECK (version >= 1),
    CONSTRAINT ck_anamnesis_respuestas CHECK (jsonb_typeof(respuestas) = 'object')
);

CREATE TABLE odontograma_registros (
    id BIGINT NOT NULL DEFAULT nextval('odontograma_registros_id_seq'),
    ficha_id BIGINT NOT NULL,
    pieza_fdi SMALLINT NOT NULL,
    cara VARCHAR(20),
    estado VARCHAR(60) NOT NULL,
    version INTEGER NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    autor_id BIGINT,
    autor_nombre VARCHAR(120) NOT NULL,
    autor_modo VARCHAR(16) NOT NULL,
    CONSTRAINT pk_odontograma_registros PRIMARY KEY (id),
    CONSTRAINT fk_odontograma_ficha FOREIGN KEY (ficha_id) REFERENCES fichas_clinicas (id),
    CONSTRAINT fk_odontograma_pieza FOREIGN KEY (pieza_fdi) REFERENCES piezas_fdi (codigo),
    CONSTRAINT ck_odontograma_version CHECK (version >= 1),
    CONSTRAINT ck_odontograma_estado CHECK (length(btrim(estado)) > 0),
    CONSTRAINT ck_odontograma_cara CHECK (cara IS NULL OR length(btrim(cara)) > 0)
);

CREATE UNIQUE INDEX ux_odontograma_version
    ON odontograma_registros (ficha_id, pieza_fdi, COALESCE(cara, ''), version);

CREATE TABLE evoluciones (
    id BIGINT NOT NULL DEFAULT nextval('evoluciones_id_seq'),
    ficha_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    turno_id BIGINT,
    version INTEGER NOT NULL,
    reemplaza_id BIGINT,
    texto TEXT NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    autor_id BIGINT,
    autor_nombre VARCHAR(120) NOT NULL,
    autor_modo VARCHAR(16) NOT NULL,
    CONSTRAINT pk_evoluciones PRIMARY KEY (id),
    CONSTRAINT fk_evoluciones_ficha FOREIGN KEY (ficha_id) REFERENCES fichas_clinicas (id),
    CONSTRAINT fk_evoluciones_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales (id),
    CONSTRAINT fk_evoluciones_turno FOREIGN KEY (turno_id) REFERENCES turnos (id),
    CONSTRAINT fk_evoluciones_reemplaza FOREIGN KEY (reemplaza_id) REFERENCES evoluciones (id),
    CONSTRAINT uq_evoluciones_reemplaza UNIQUE (reemplaza_id),
    CONSTRAINT ck_evoluciones_version CHECK (version >= 1),
    CONSTRAINT ck_evoluciones_texto CHECK (length(btrim(texto)) > 0)
);

CREATE TABLE consentimientos (
    id BIGINT NOT NULL DEFAULT nextval('consentimientos_id_seq'),
    ficha_id BIGINT NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    texto TEXT NOT NULL,
    version INTEGER NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    autor_id BIGINT,
    autor_nombre VARCHAR(120) NOT NULL,
    autor_modo VARCHAR(16) NOT NULL,
    CONSTRAINT pk_consentimientos PRIMARY KEY (id),
    CONSTRAINT fk_consentimientos_ficha FOREIGN KEY (ficha_id) REFERENCES fichas_clinicas (id),
    CONSTRAINT uq_consentimientos_version UNIQUE (ficha_id, tipo, version),
    CONSTRAINT ck_consentimientos_version CHECK (version >= 1),
    CONSTRAINT ck_consentimientos_tipo CHECK (length(btrim(tipo)) > 0),
    CONSTRAINT ck_consentimientos_texto CHECK (length(btrim(texto)) > 0)
);

CREATE FUNCTION historia_clinica_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% es append-only: la historia clínica se versiona, no se sobrescribe', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_anamnesis_append_only BEFORE UPDATE OR DELETE ON anamnesis
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
CREATE TRIGGER tr_odontograma_append_only BEFORE UPDATE OR DELETE ON odontograma_registros
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
CREATE TRIGGER tr_evoluciones_append_only BEFORE UPDATE OR DELETE ON evoluciones
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
CREATE TRIGGER tr_consentimientos_append_only BEFORE UPDATE OR DELETE ON consentimientos
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
CREATE TRIGGER tr_piezas_fdi_append_only BEFORE UPDATE OR DELETE ON piezas_fdi
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();

CREATE FUNCTION fichas_clinicas_sin_borrado() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'fichas_clinicas no admite borrado físico (TBD, RN-CL-05)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_fichas_clinicas_sin_borrado BEFORE DELETE ON fichas_clinicas
    FOR EACH ROW EXECUTE FUNCTION fichas_clinicas_sin_borrado();
