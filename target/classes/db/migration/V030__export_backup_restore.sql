-- C-17 export-backup-restore (Parte 4, rango reservado V030+).
-- Registro de exportaciones, respaldos y restauraciones verificadas en entorno aislado.
-- Formato oficial, destino, frecuencia (BACKUP_CRON), plazo de copia y retención siguen (TBD, IN-05/IN-06):
-- se guardan por operación cuando están configurados; nunca se inventan por defecto (RN-GL-03).
CREATE SEQUENCE cumplimiento_operaciones_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE cumplimiento_operaciones (
    id BIGINT NOT NULL DEFAULT nextval('cumplimiento_operaciones_id_seq'),
    tipo VARCHAR(20) NOT NULL,
    alcance VARCHAR(20) NOT NULL,
    paciente_id BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA',
    formato VARCHAR(40) NOT NULL,
    destino VARCHAR(255),
    cifrado BOOLEAN NOT NULL DEFAULT FALSE,
    respaldo_origen_id BIGINT,
    entorno_restore VARCHAR(255),
    motivo VARCHAR(255),
    solicitado_por VARCHAR(120) NOT NULL,
    solicitado_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    plazo_vence_at TIMESTAMPTZ,
    completado_at TIMESTAMPTZ,
    retener_hasta TIMESTAMPTZ,
    checksum_sha256 CHAR(64),
    tablas INTEGER,
    filas BIGINT,
    error VARCHAR(500),
    CONSTRAINT pk_cumplimiento_operaciones PRIMARY KEY (id),
    CONSTRAINT fk_cumplimiento_operaciones_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_cumplimiento_operaciones_respaldo FOREIGN KEY (respaldo_origen_id)
        REFERENCES cumplimiento_operaciones (id),
    CONSTRAINT ck_cumplimiento_operaciones_tipo CHECK (tipo IN ('EXPORTACION', 'RESPALDO', 'RESTORE')),
    CONSTRAINT ck_cumplimiento_operaciones_alcance CHECK (alcance IN ('COMPLETA', 'PACIENTE')),
    CONSTRAINT ck_cumplimiento_operaciones_estado CHECK (estado IN ('SOLICITADA', 'COMPLETADA', 'FALLIDA')),
    CONSTRAINT ck_cumplimiento_operaciones_paciente CHECK (alcance <> 'PACIENTE' OR paciente_id IS NOT NULL),
    CONSTRAINT ck_cumplimiento_operaciones_paciente_solo_export
        CHECK (alcance = 'COMPLETA' OR tipo = 'EXPORTACION'),
    CONSTRAINT ck_cumplimiento_operaciones_restore
        CHECK (tipo <> 'RESTORE' OR (respaldo_origen_id IS NOT NULL AND entorno_restore IS NOT NULL)),
    CONSTRAINT ck_cumplimiento_operaciones_completada
        CHECK (estado <> 'COMPLETADA' OR (completado_at IS NOT NULL AND checksum_sha256 IS NOT NULL)),
    CONSTRAINT ck_cumplimiento_operaciones_fallida
        CHECK (estado <> 'FALLIDA' OR length(btrim(error)) > 0),
    CONSTRAINT ck_cumplimiento_operaciones_cifrado
        CHECK (estado <> 'COMPLETADA' OR tipo = 'RESTORE' OR cifrado)
);

CREATE INDEX ix_cumplimiento_operaciones_tipo_estado ON cumplimiento_operaciones (tipo, estado);
CREATE INDEX ix_cumplimiento_operaciones_plazo ON cumplimiento_operaciones (plazo_vence_at)
    WHERE estado = 'SOLICITADA';

-- Una operación solo transiciona SOLICITADA -> COMPLETADA | FALLIDA; los estados finales son inmutables
-- y ninguna fila se borra: el registro es evidencia (RN-SE-03, RN-SE-04).
CREATE OR REPLACE FUNCTION cumplimiento_operaciones_protege_registro() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'cumplimiento_operaciones no admite DELETE';
    END IF;
    IF OLD.estado <> 'SOLICITADA' THEN
        RAISE EXCEPTION 'operación % en estado final %: no se modifica', OLD.id, OLD.estado;
    END IF;
    IF NEW.tipo <> OLD.tipo OR NEW.alcance <> OLD.alcance
        OR NEW.paciente_id IS DISTINCT FROM OLD.paciente_id
        OR NEW.solicitado_at <> OLD.solicitado_at OR NEW.solicitado_por <> OLD.solicitado_por
        OR NEW.respaldo_origen_id IS DISTINCT FROM OLD.respaldo_origen_id THEN
        RAISE EXCEPTION 'operación %: los datos de la solicitud no se modifican', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cumplimiento_operaciones_update
    BEFORE UPDATE ON cumplimiento_operaciones
    FOR EACH ROW EXECUTE FUNCTION cumplimiento_operaciones_protege_registro();

CREATE TRIGGER trg_cumplimiento_operaciones_delete
    BEFORE DELETE ON cumplimiento_operaciones
    FOR EACH ROW EXECUTE FUNCTION cumplimiento_operaciones_protege_registro();
