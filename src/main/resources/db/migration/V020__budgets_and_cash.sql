-- C-14 budgets-and-cash: presupuestos e ítems a precio snapshot.
-- Parte 3 de la reserva de numeración Flyway (desde V020). No se edita V001-V006.

CREATE SEQUENCE presupuestos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE items_presupuesto_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE presupuestos (
    id BIGINT NOT NULL DEFAULT nextval('presupuestos_id_seq'),
    paciente_id BIGINT NOT NULL,
    turno_id BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'emitido',
    moneda VARCHAR(3) NOT NULL DEFAULT 'ARS',
    total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_presupuestos PRIMARY KEY (id),
    CONSTRAINT fk_presupuestos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_presupuestos_turno FOREIGN KEY (turno_id) REFERENCES turnos (id),
    CONSTRAINT ck_presupuestos_estado CHECK (estado IN ('emitido', 'anulado')),
    CONSTRAINT ck_presupuestos_moneda CHECK (moneda = 'ARS'),
    CONSTRAINT ck_presupuestos_total_no_negativo CHECK (total >= 0)
);

CREATE TABLE items_presupuesto (
    id BIGINT NOT NULL DEFAULT nextval('items_presupuesto_id_seq'),
    presupuesto_id BIGINT NOT NULL,
    tratamiento_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    precio NUMERIC(12,2) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_items_presupuesto PRIMARY KEY (id),
    CONSTRAINT fk_items_presupuesto_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES presupuestos (id),
    CONSTRAINT fk_items_presupuesto_tratamiento FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id),
    CONSTRAINT ck_items_presupuesto_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_items_presupuesto_precio CHECK (precio >= 0),
    CONSTRAINT ck_items_presupuesto_descripcion CHECK (length(btrim(descripcion)) > 0)
);

CREATE INDEX ix_presupuestos_paciente ON presupuestos (paciente_id);
CREATE INDEX ix_presupuestos_turno ON presupuestos (turno_id);
CREATE INDEX ix_items_presupuesto_presupuesto ON items_presupuesto (presupuesto_id);
