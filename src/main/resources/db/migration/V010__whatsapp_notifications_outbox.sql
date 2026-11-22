-- C-10 whatsapp-notifications-outbox (Parte 2, rango V010+).
-- Plantillas aprobadas por Meta (codigos (TBD): sin seed), notificaciones con estado e intentos,
-- outbox transaccional, registro de intentos y webhooks idempotentes (RN-NT-01, RN-NT-03, RN-GL-02).

CREATE SEQUENCE plantillas_whatsapp_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notificaciones_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notificacion_intentos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notificaciones_outbox_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE whatsapp_webhook_eventos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE plantillas_whatsapp (
    id BIGINT NOT NULL DEFAULT nextval('plantillas_whatsapp_id_seq'),
    canal VARCHAR(20) NOT NULL DEFAULT 'whatsapp',
    codigo_meta VARCHAR(120) NOT NULL,
    proposito VARCHAR(20) NOT NULL,
    idioma VARCHAR(10) NOT NULL,
    estado_aprobacion VARCHAR(20) NOT NULL,
    motivo_rechazo VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_plantillas_whatsapp PRIMARY KEY (id),
    CONSTRAINT uq_plantillas_whatsapp_codigo_idioma UNIQUE (codigo_meta, idioma),
    CONSTRAINT ck_plantillas_whatsapp_canal CHECK (canal = 'whatsapp'),
    CONSTRAINT ck_plantillas_whatsapp_proposito CHECK (proposito IN ('CONFIRMACION', 'RECORDATORIO')),
    CONSTRAINT ck_plantillas_whatsapp_estado CHECK (estado_aprobacion IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_plantillas_whatsapp_codigo CHECK (length(btrim(codigo_meta)) > 0)
);

CREATE TABLE notificaciones (
    id BIGINT NOT NULL DEFAULT nextval('notificaciones_id_seq'),
    turno_id BIGINT NOT NULL,
    plantilla_id BIGINT,
    proposito VARCHAR(20) NOT NULL,
    destinatario VARCHAR(40) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    intentos INTEGER NOT NULL DEFAULT 0,
    proveedor_message_id VARCHAR(160),
    programada_para TIMESTAMPTZ NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    ultimo_error VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_notificaciones PRIMARY KEY (id),
    CONSTRAINT fk_notificaciones_turno FOREIGN KEY (turno_id) REFERENCES turnos (id),
    CONSTRAINT fk_notificaciones_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantillas_whatsapp (id),
    CONSTRAINT uq_notificaciones_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_notificaciones_proveedor_message UNIQUE (proveedor_message_id),
    CONSTRAINT ck_notificaciones_proposito CHECK (proposito IN ('CONFIRMACION', 'RECORDATORIO')),
    CONSTRAINT ck_notificaciones_estado CHECK (estado IN
        ('PENDIENTE', 'ENVIADA', 'ENTREGADA', 'LEIDA', 'CONFIRMADA', 'FALLIDA', 'BLOQUEADA')),
    CONSTRAINT ck_notificaciones_intentos CHECK (intentos >= 0),
    CONSTRAINT ck_notificaciones_destinatario CHECK (length(btrim(destinatario)) > 0)
);

CREATE INDEX ix_notificaciones_turno ON notificaciones (turno_id);
CREATE INDEX ix_notificaciones_estado ON notificaciones (estado);

CREATE TABLE notificaciones_outbox (
    id BIGINT NOT NULL DEFAULT nextval('notificaciones_outbox_id_seq'),
    notificacion_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    proximo_intento TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    procesado_at TIMESTAMPTZ,
    CONSTRAINT pk_notificaciones_outbox PRIMARY KEY (id),
    CONSTRAINT fk_notificaciones_outbox_notificacion FOREIGN KEY (notificacion_id) REFERENCES notificaciones (id),
    CONSTRAINT uq_notificaciones_outbox_notificacion UNIQUE (notificacion_id),
    CONSTRAINT ck_notificaciones_outbox_estado CHECK (estado IN ('PENDIENTE', 'PROCESADO'))
);

CREATE INDEX ix_notificaciones_outbox_pendientes ON notificaciones_outbox (proximo_intento)
    WHERE estado = 'PENDIENTE';

CREATE TABLE notificacion_intentos (
    id BIGINT NOT NULL DEFAULT nextval('notificacion_intentos_id_seq'),
    notificacion_id BIGINT NOT NULL,
    numero INTEGER NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    resultado VARCHAR(20) NOT NULL,
    proveedor_message_id VARCHAR(160),
    error VARCHAR(255),
    CONSTRAINT pk_notificacion_intentos PRIMARY KEY (id),
    CONSTRAINT fk_notificacion_intentos_notificacion FOREIGN KEY (notificacion_id) REFERENCES notificaciones (id),
    CONSTRAINT uq_notificacion_intentos_numero UNIQUE (notificacion_id, numero),
    CONSTRAINT ck_notificacion_intentos_resultado CHECK (resultado IN ('EXITO', 'ERROR', 'BLOQUEADO'))
);

CREATE TABLE whatsapp_webhook_eventos (
    id BIGINT NOT NULL DEFAULT nextval('whatsapp_webhook_eventos_id_seq'),
    evento_id VARCHAR(200) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    proveedor_message_id VARCHAR(160),
    recibido_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_whatsapp_webhook_eventos PRIMARY KEY (id),
    CONSTRAINT uq_whatsapp_webhook_eventos_evento UNIQUE (evento_id)
);

-- Append-only: ni intentos ni eventos de webhook se modifican o borran.
CREATE FUNCTION notificaciones_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% es append-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_notificacion_intentos_append_only
    BEFORE UPDATE OR DELETE ON notificacion_intentos
    FOR EACH ROW EXECUTE FUNCTION notificaciones_append_only();

CREATE TRIGGER tr_whatsapp_webhook_eventos_append_only
    BEFORE UPDATE OR DELETE ON whatsapp_webhook_eventos
    FOR EACH ROW EXECUTE FUNCTION notificaciones_append_only();
