-- C-12 clinical-signature-consent (Parte 2).
-- Firma de evolución con matrícula del profesional (RN-CL-03) y firma electrónica de
-- consentimientos conforme Ley 25.506 vía proveedor (TBD). Las firmas son append-only y
-- guardan el hash del contenido firmado: modificar lo firmado exige una versión nueva (RN-CL-02).

CREATE SEQUENCE evolucion_firmas_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE consentimiento_firmas_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE evolucion_firmas (
    id BIGINT NOT NULL DEFAULT nextval('evolucion_firmas_id_seq'),
    evolucion_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    matricula VARCHAR(40) NOT NULL,
    hash_contenido CHAR(64) NOT NULL,
    firmada_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    firmante_id BIGINT,
    firmante_nombre VARCHAR(120) NOT NULL,
    firmante_modo VARCHAR(16) NOT NULL,
    CONSTRAINT pk_evolucion_firmas PRIMARY KEY (id),
    CONSTRAINT uq_evolucion_firmas_evolucion UNIQUE (evolucion_id),
    CONSTRAINT fk_evolucion_firmas_evolucion FOREIGN KEY (evolucion_id) REFERENCES evoluciones (id),
    CONSTRAINT fk_evolucion_firmas_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales (id),
    CONSTRAINT ck_evolucion_firmas_matricula CHECK (length(btrim(matricula)) > 0),
    CONSTRAINT ck_evolucion_firmas_hash CHECK (hash_contenido ~ '^[0-9a-f]{64}$')
);

CREATE TABLE consentimiento_firmas (
    id BIGINT NOT NULL DEFAULT nextval('consentimiento_firmas_id_seq'),
    consentimiento_id BIGINT NOT NULL,
    proveedor VARCHAR(60) NOT NULL,
    referencia_proveedor VARCHAR(200) NOT NULL,
    hash_documento CHAR(64) NOT NULL,
    firmada_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    registrado_por_id BIGINT,
    registrado_por_nombre VARCHAR(120) NOT NULL,
    registrado_por_modo VARCHAR(16) NOT NULL,
    CONSTRAINT pk_consentimiento_firmas PRIMARY KEY (id),
    CONSTRAINT uq_consentimiento_firmas_consentimiento UNIQUE (consentimiento_id),
    CONSTRAINT fk_consentimiento_firmas_consentimiento FOREIGN KEY (consentimiento_id) REFERENCES consentimientos (id),
    CONSTRAINT ck_consentimiento_firmas_referencia CHECK (length(btrim(referencia_proveedor)) > 0),
    CONSTRAINT ck_consentimiento_firmas_hash CHECK (hash_documento ~ '^[0-9a-f]{64}$')
);

CREATE TRIGGER tr_evolucion_firmas_append_only BEFORE UPDATE OR DELETE ON evolucion_firmas
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
CREATE TRIGGER tr_consentimiento_firmas_append_only BEFORE UPDATE OR DELETE ON consentimiento_firmas
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
