-- C-15 mercadopago-deposits-reconciliation: seña y pago de Mercado Pago.
-- Parte 3 de la reserva Flyway, después de V020 (C-14). No se editan V001-V020.
-- La resolución referencial se hace en la capa de dominio (MP-3): turno_id y
-- presupuesto_id son asociaciones opcionales, sin FK física, para poder detectar
-- referencias inexistentes y dejar la seña sin conciliar en lugar de perder el evento.
-- No se crean tablas de caja, asiento ni arqueo (MP-3).

CREATE SEQUENCE senias_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE senias (
    id BIGINT NOT NULL DEFAULT nextval('senias_id_seq'),
    turno_id BIGINT,
    presupuesto_id BIGINT,
    monto NUMERIC(12,2) NOT NULL,
    moneda VARCHAR(3) NOT NULL DEFAULT 'ARS',
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    proveedor VARCHAR(30) NOT NULL DEFAULT 'mercadopago',
    payment_id VARCHAR(64),
    external_reference VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    fecha_aprobacion TIMESTAMPTZ,
    conciliado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_senias PRIMARY KEY (id),
    CONSTRAINT ck_senias_monto_positivo CHECK (monto > 0),
    CONSTRAINT ck_senias_moneda_ars CHECK (moneda = 'ARS'),
    CONSTRAINT ck_senias_estado CHECK (estado IN
        ('pendiente', 'aprobado', 'rechazado', 'cancelado', 'reembolsado')),
    CONSTRAINT ck_senias_proveedor CHECK (proveedor = 'mercadopago'),
    CONSTRAINT ck_senias_referencia_no_vacia CHECK (length(btrim(external_reference)) > 0),
    CONSTRAINT ck_senias_clave_no_vacia CHECK (length(btrim(idempotency_key)) > 0)
);

CREATE UNIQUE INDEX uq_senias_external_reference ON senias (external_reference);
CREATE UNIQUE INDEX uq_senias_idempotency_key ON senias (idempotency_key);
CREATE INDEX ix_senias_turno ON senias (turno_id);
CREATE INDEX ix_senias_presupuesto ON senias (presupuesto_id);
CREATE INDEX ix_senias_estado ON senias (estado);
