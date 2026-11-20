CREATE SEQUENCE consultorios_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE pacientes_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE profesionales_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE sillones_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE usuarios_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE roles_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE permisos_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE consultorios (
    id BIGINT NOT NULL DEFAULT nextval('consultorios_id_seq'),
    nombre VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_consultorios PRIMARY KEY (id)
);

CREATE TABLE pacientes (
    id BIGINT NOT NULL DEFAULT nextval('pacientes_id_seq'),
    documento_tipo VARCHAR(16) NOT NULL,
    documento_numero VARCHAR(32) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    apellido VARCHAR(120) NOT NULL,
    telefono VARCHAR(40) NOT NULL,
    fecha_nacimiento DATE,
    email VARCHAR(160),
    direccion VARCHAR(255),
    consentimiento_datos BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_consentimiento TIMESTAMPTZ,
    version_politica VARCHAR(40),
    finalidad VARCHAR(255),
    canal VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_pacientes PRIMARY KEY (id),
    CONSTRAINT ck_pacientes_documento_tipo CHECK (documento_tipo IN ('DNI', 'CUIT', 'PASAPORTE'))
);

CREATE TABLE profesionales (
    id BIGINT NOT NULL DEFAULT nextval('profesionales_id_seq'),
    matricula VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    apellido VARCHAR(120) NOT NULL,
    especialidad VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_profesionales PRIMARY KEY (id),
    CONSTRAINT uq_profesionales_matricula UNIQUE (matricula)
);

CREATE TABLE sillones (
    id BIGINT NOT NULL DEFAULT nextval('sillones_id_seq'),
    numero VARCHAR(40) NOT NULL,
    consultorio_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_sillones PRIMARY KEY (id),
    CONSTRAINT fk_sillones_consultorio FOREIGN KEY (consultorio_id) REFERENCES consultorios (id)
);

CREATE TABLE roles (
    id BIGINT NOT NULL DEFAULT nextval('roles_id_seq'),
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_codigo UNIQUE (codigo)
);

CREATE TABLE permisos (
    id BIGINT NOT NULL DEFAULT nextval('permisos_id_seq'),
    recurso VARCHAR(60) NOT NULL,
    accion VARCHAR(20) NOT NULL,
    ambito VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_permisos PRIMARY KEY (id),
    CONSTRAINT uq_permisos_recurso_accion_ambito UNIQUE (recurso, accion, ambito),
    CONSTRAINT ck_permisos_ambito CHECK (ambito IN ('propio', 'todos', 'config'))
);

CREATE TABLE rol_permiso (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_rol_permiso PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permisos (id)
);

CREATE TABLE usuarios (
    id BIGINT NOT NULL DEFAULT nextval('usuarios_id_seq'),
    rol_id BIGINT NOT NULL,
    email VARCHAR(160) NOT NULL,
    hash_password VARCHAR(255),
    proveedor VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ultimo_acceso TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(120),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
);

CREATE INDEX ix_pacientes_telefono ON pacientes (telefono);
CREATE UNIQUE INDEX ux_pacientes_documento ON pacientes (documento_tipo, documento_numero) WHERE activo = true;
CREATE INDEX ix_profesionales_activo ON profesionales (activo) WHERE activo = true;
CREATE INDEX ix_sillones_activo ON sillones (activo) WHERE activo = true;

INSERT INTO consultorios (id, nombre) VALUES (1, 'Consultorio principal');
SELECT setval('consultorios_id_seq', 1, true);

INSERT INTO roles (id, codigo, nombre) VALUES
    (1, 'paciente', 'Paciente'),
    (2, 'recepcionista', 'Recepcionista'),
    (3, 'odontologo', 'Odontólogo'),
    (4, 'administrador', 'Administrador'),
    (5, 'auditor', 'Auditor');
SELECT setval('roles_id_seq', 5, true);

INSERT INTO permisos (id, recurso, accion, ambito) VALUES
    (1, 'agenda', 'leer', 'propio'),
    (2, 'agenda', 'crear', 'todos'),
    (3, 'agenda', 'actualizar', 'todos'),
    (4, 'turno', 'leer', 'propio'),
    (5, 'turno', 'crear', 'propio'),
    (6, 'turno', 'actualizar', 'propio'),
    (7, 'turno', 'leer', 'todos'),
    (8, 'ficha_clinica', 'leer', 'propio'),
    (9, 'ficha_clinica', 'crear', 'todos'),
    (10, 'auditoria', 'leer', 'todos'),
    (11, 'configuracion', 'leer', 'config'),
    (12, 'exportacion', 'crear', 'todos');
SELECT setval('permisos_id_seq', 12, true);

INSERT INTO rol_permiso (rol_id, permiso_id) VALUES
    (1, 1), (1, 4), (1, 5), (1, 6), (1, 8),
    (2, 2), (2, 3), (2, 7), (2, 9),
    (3, 1), (3, 4), (3, 8), (3, 9),
    (4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 11), (4, 12),
    (5, 10), (5, 11);
