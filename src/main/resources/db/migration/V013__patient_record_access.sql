-- C-13 patient-clinical-record-access (Parte 2).
-- Registro de cada copia de la historia entregada al titular (Ley 26.529 art. 14, RN-CL-04):
-- cuándo se pidió, el vencimiento del plazo operativo (TBD, configurable), cuándo se entregó y
-- el hash del documento. Append-only. Los accesos y denegaciones van al log de auditoría (C-05).

CREATE SEQUENCE historia_copias_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE historia_copias (
    id BIGINT NOT NULL DEFAULT nextval('historia_copias_id_seq'),
    paciente_id BIGINT NOT NULL,
    solicitada_at TIMESTAMPTZ NOT NULL,
    vence_at TIMESTAMPTZ NOT NULL,
    entregada_at TIMESTAMPTZ NOT NULL,
    hash_documento CHAR(64) NOT NULL,
    token_jti VARCHAR(120) NOT NULL,
    CONSTRAINT pk_historia_copias PRIMARY KEY (id),
    CONSTRAINT fk_historia_copias_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT ck_historia_copias_plazo CHECK (vence_at > solicitada_at),
    CONSTRAINT ck_historia_copias_entrega CHECK (entregada_at >= solicitada_at),
    CONSTRAINT ck_historia_copias_hash CHECK (hash_documento ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_historia_copias_paciente ON historia_copias (paciente_id);

CREATE TRIGGER tr_historia_copias_append_only BEFORE UPDATE OR DELETE ON historia_copias
    FOR EACH ROW EXECUTE FUNCTION historia_clinica_append_only();
