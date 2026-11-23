-- C-16 arca-invoicing-reconciliation: comprobantes ARCA sobre seña cobrada.
-- Parte 3 de la reserva Flyway, después de V021 (C-15). No se editan V001-V021.
-- La unicidad es por (senia_id, tipo): garantiza como máximo una factura por seña
-- y permite la nota de crédito manual sobre la misma seña reembolsada (AR-1, AR-11).
-- presupuesto_id y turno_id son opcionales, sin FK física (mismo patrón que V021).
-- No se crean tablas de renglones, impuestos, caja, asiento ni arqueo (AR-2, AR-3).

CREATE SEQUENCE comprobantes_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE comprobantes (
    id BIGINT NOT NULL DEFAULT nextval('comprobantes_id_seq'),
    senia_id BIGINT NOT NULL,
    presupuesto_id BIGINT,
    turno_id BIGINT,
    tipo VARCHAR(20) NOT NULL DEFAULT 'factura',
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    moneda VARCHAR(3) NOT NULL DEFAULT 'ARS',
    monto NUMERIC(12,2) NOT NULL,
    concepto VARCHAR(255) NOT NULL,
    cae VARCHAR(64),
    fecha_emision TIMESTAMPTZ,
    intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(2000),
    conciliado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_comprobantes PRIMARY KEY (id),
    CONSTRAINT fk_comprobantes_senia FOREIGN KEY (senia_id) REFERENCES senias (id),
    CONSTRAINT uq_comprobantes_senia_tipo UNIQUE (senia_id, tipo),
    CONSTRAINT ck_comprobantes_tipo CHECK (tipo IN ('factura', 'nota_credito')),
    CONSTRAINT ck_comprobantes_estado CHECK (estado IN ('pendiente', 'emitido', 'rechazado', 'anulado')),
    CONSTRAINT ck_comprobantes_moneda CHECK (moneda = 'ARS'),
    CONSTRAINT ck_comprobantes_monto CHECK (monto > 0),
    CONSTRAINT ck_comprobantes_concepto CHECK (length(btrim(concepto)) > 0),
    CONSTRAINT ck_comprobantes_intentos CHECK (intentos >= 0)
);

CREATE INDEX ix_comprobantes_senia ON comprobantes (senia_id);
CREATE INDEX ix_comprobantes_presupuesto ON comprobantes (presupuesto_id);
CREATE INDEX ix_comprobantes_turno ON comprobantes (turno_id);
CREATE INDEX ix_comprobantes_estado ON comprobantes (estado);
CREATE INDEX ix_comprobantes_tipo ON comprobantes (tipo);
