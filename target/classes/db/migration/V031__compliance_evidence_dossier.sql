-- C-18 compliance-evidence-dossier (Parte 4, rango reservado V030+).
-- Dossier mínimo con listado explícito de pendientes (C-01 decisión D=b, cumplimiento progresivo).
-- Los ítems se siembran PENDIENTES: ninguna evidencia, responsable ni owner legal se inventa (RN-GL-03).
CREATE TABLE dossier_items (
    codigo VARCHAR(60) NOT NULL,
    orden INTEGER NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    categoria VARCHAR(40) NOT NULL,
    fuente VARCHAR(20) NOT NULL,
    base_normativa VARCHAR(255) NOT NULL,
    obligatorio BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    evidencia_referencia VARCHAR(500),
    verificado_por VARCHAR(120),
    verificado_at TIMESTAMPTZ,
    CONSTRAINT pk_dossier_items PRIMARY KEY (codigo),
    CONSTRAINT uq_dossier_items_orden UNIQUE (orden),
    CONSTRAINT ck_dossier_items_fuente CHECK (fuente IN ('MANUAL', 'AUTOMATICA')),
    CONSTRAINT ck_dossier_items_estado CHECK (estado IN ('PENDIENTE', 'PRESENTE')),
    CONSTRAINT ck_dossier_items_automatica_sin_estado CHECK (fuente = 'MANUAL' OR estado = 'PENDIENTE'),
    CONSTRAINT ck_dossier_items_evidencia CHECK (
        estado <> 'PRESENTE'
        OR (length(btrim(evidencia_referencia)) > 0 AND length(btrim(verificado_por)) > 0
            AND verificado_at IS NOT NULL))
);

CREATE SEQUENCE subprocesadores_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE subprocesadores (
    id BIGINT NOT NULL DEFAULT nextval('subprocesadores_id_seq'),
    nombre VARCHAR(120) NOT NULL,
    servicio VARCHAR(255) NOT NULL,
    ubicacion_datos VARCHAR(120),
    clausulas_aaip_referencia VARCHAR(500),
    dpa_referencia VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120) NOT NULL,
    CONSTRAINT pk_subprocesadores PRIMARY KEY (id),
    CONSTRAINT uq_subprocesadores_nombre UNIQUE (nombre),
    CONSTRAINT ck_subprocesadores_nombre CHECK (length(btrim(nombre)) > 0),
    CONSTRAINT ck_subprocesadores_servicio CHECK (length(btrim(servicio)) > 0)
);

-- Ítems exigidos según RN-SE-04/RN-SE-05 y C-01 D. Los AUTOMATICOS se evalúan contra el sistema
-- (RBAC, cadena de auditoría, exportación, respaldo/restore, subprocesadores); los MANUALES requieren
-- evidencia registrada por una persona con nombre y fecha.
INSERT INTO dossier_items (codigo, orden, descripcion, categoria, fuente, base_normativa) VALUES
    ('OWNER_LEGAL', 1, 'Owner legal que firma el dossier y responsable de datos designados',
        'GOBIERNO', 'MANUAL', 'C-01 decisión D; IN-05'),
    ('MARCO_NORMATIVO', 2, 'Aplicabilidad confirmada de Ley 25.326, Ley 26.529, Decreto 1089/2012 y Ley 25.506',
        'GOBIERNO', 'MANUAL', 'C-01 decisión D; IN-06'),
    ('REGISTRO_RNBD_AAIP', 3, 'Inscripción de la base en el RNBD/AAIP por el responsable',
        'REGISTRO', 'MANUAL', 'Ley 25.326 art. 21; RN-SE-04'),
    ('DPA_RESPONSABLE_ENCARGADO', 4, 'Contrato de tratamiento (DPA) entre responsable y encargado',
        'CONTRATOS', 'MANUAL', 'Ley 25.326 art. 25; RN-SE-04'),
    ('PROCEDIMIENTO_INTIMA_CONSTA', 5, 'Procedimiento de ejercicio de derechos (INTIMA/CONSTA)',
        'DERECHOS', 'MANUAL', 'Ley 25.326 arts. 14-16; RN-SE-04'),
    ('POLITICA_RETENCION', 6, 'Política de retención de historia clínica y respaldos aprobada',
        'RETENCION', 'MANUAL', 'Ley 26.529 art. 18; RN-SE-03'),
    ('UBICACION_DATOS', 7, 'Ubicación de infraestructura y cifrado en reposo documentados',
        'SEGURIDAD', 'MANUAL', 'RN-SE-05; C-01 B6'),
    ('ROLES_RBAC', 8, 'Matriz de roles y permisos vigente',
        'SEGURIDAD', 'AUTOMATICA', 'RN-SE-01; RN-SE-04'),
    ('LOG_AUDITORIA_INTEGRO', 9, 'Log append-only con cadena de hash verificada',
        'TRAZABILIDAD', 'AUTOMATICA', 'RN-SE-02; RN-SE-04'),
    ('EXPORTACION_COMPLETA', 10, 'Exportación completa de datos generada y cifrada',
        'PORTABILIDAD', 'AUTOMATICA', 'RN-SE-03; RN-SE-04'),
    ('RESPALDO_RESTORE_VERIFICADO', 11, 'Respaldo cifrado con restore verificado en entorno aislado',
        'CONTINUIDAD', 'AUTOMATICA', 'RN-SE-03; RN-SE-05'),
    ('CLAUSULAS_SUBPROCESADORES', 12, 'Subprocesadores con cláusulas AAIP 198/2023 publicadas',
        'CONTRATOS', 'AUTOMATICA', 'Disposición AAIP 198/2023; RN-SE-04');
