-- =====================================================================================
-- Datos de prueba (solo entorno dev). NO es una migración de producción.
--
-- Se carga automáticamente con el perfil `dev` (ver application-dev.yml, ubicación
-- classpath:db/datos-prueba) o a mano con psql DESPUÉS de aplicar V001..V031:
--     psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f src/main/resources/db/datos-prueba/V900__datos_de_prueba.sql
--
-- Todas las fechas de agenda se calculan RELATIVAS al día de carga (días hábiles,
-- saltando sábados, domingos y feriados), para que siempre haya turnos pasados,
-- de hoy, dentro de la ventana de 48 hs hábiles y fuera de ella.
--
-- Todos los datos personales son FICTICIOS. Teléfonos, documentos, CUIT, matrículas,
-- CAE, payment_id y message_id son inventados y marcados como de prueba.
-- No se siembra auditoria_log (su cadena de hash la genera la aplicación), ni tokens
-- públicos firmados (dependen del secreto del entorno), ni evidencia de cumplimiento
-- COMPLETADA (no se inventa evidencia: se genera ejecutando las operaciones reales).
--
-- Credenciales de todos los usuarios de staff: contraseña  Prueba1234!
-- Mapa completo de escenarios: docs/datos-de-prueba.md
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. Recursos físicos: sillones (el consultorio 1 ya existe por V001)
-- -------------------------------------------------------------------------------------
UPDATE consultorios SET nombre = 'Consultorio Odontológico Demo' WHERE id = 1;

INSERT INTO sillones (id, numero, consultorio_id, created_by, activo) VALUES
    (1, 'Sillón 1', 1, 'datos-de-prueba', TRUE),
    (2, 'Sillón 2', 1, 'datos-de-prueba', TRUE),
    (3, 'Sillón 3', 1, 'datos-de-prueba', TRUE),
    (4, 'Sillón 4 (fuera de servicio)', 1, 'datos-de-prueba', FALSE);

-- -------------------------------------------------------------------------------------
-- 2. Profesionales (matrículas ficticias)
-- -------------------------------------------------------------------------------------
INSERT INTO profesionales (id, matricula, nombre, apellido, especialidad, created_by, activo) VALUES
    (1, 'MN-PRUEBA-10001', 'Laura',    'Giménez',   'Odontología general', 'datos-de-prueba', TRUE),
    (2, 'MP-PRUEBA-20002', 'Martín',   'Sosa',      'Ortodoncia',          'datos-de-prueba', TRUE),
    (3, 'MN-PRUEBA-30003', 'Carolina', 'Ruiz',      'Odontopediatría',     'datos-de-prueba', TRUE),
    (4, 'MP-PRUEBA-40004', 'Diego',    'Fernández', 'Endodoncia',          'datos-de-prueba', TRUE),
    (5, 'MN-PRUEBA-50005', 'Ana',      'Paz',       'Periodoncia',         'datos-de-prueba', FALSE);

-- -------------------------------------------------------------------------------------
-- 3. Catálogo de tratamientos con duración variable (RN-AG-03)
-- -------------------------------------------------------------------------------------
INSERT INTO tratamientos (id, codigo, nombre, duracion_minutos, created_by, activo) VALUES
    (1,  'CONSULTA',     'Consulta y diagnóstico',            30,  'datos-de-prueba', TRUE),
    (2,  'LIMPIEZA',     'Limpieza y profilaxis',             45,  'datos-de-prueba', TRUE),
    (3,  'OBTURACION',   'Obturación (arreglo)',              60,  'datos-de-prueba', TRUE),
    (4,  'ENDODONCIA',   'Tratamiento de conducto',           90,  'datos-de-prueba', TRUE),
    (5,  'EXTRACCION',   'Extracción simple',                 45,  'datos-de-prueba', TRUE),
    (6,  'ORTO-CONTROL', 'Control de ortodoncia',             20,  'datos-de-prueba', TRUE),
    (7,  'ORTO-INSTAL',  'Instalación de aparatología',       120, 'datos-de-prueba', TRUE),
    (8,  'BLANQUEAMIENTO','Blanqueamiento',                   60,  'datos-de-prueba', TRUE),
    (9,  'PED-CONTROL',  'Control odontopediátrico',          30,  'datos-de-prueba', TRUE),
    (10, 'SELLADORES',   'Selladores de fosas y fisuras',     30,  'datos-de-prueba', TRUE),
    (11, 'IMPLANTE-EVAL','Evaluación para implante',          40,  'datos-de-prueba', TRUE),
    (12, 'URGENCIA',     'Atención de urgencia',              30,  'datos-de-prueba', TRUE),
    (13, 'PROTESIS-OLD', 'Prótesis removible (discontinuado)', 60, 'datos-de-prueba', FALSE);

-- -------------------------------------------------------------------------------------
-- 4. Horarios de atención (dia_semana ISO: 1 = lunes ... 7 = domingo)
-- -------------------------------------------------------------------------------------
INSERT INTO horarios (id, profesional_id, dia_semana, desde, hasta, created_by, activo) VALUES
    -- Laura Giménez: lunes a viernes, mañana y tarde
    (1, 1, 1, '09:00', '13:00', 'datos-de-prueba', TRUE),
    (2, 1, 1, '14:00', '18:00', 'datos-de-prueba', TRUE),
    (3, 1, 2, '09:00', '13:00', 'datos-de-prueba', TRUE),
    (4, 1, 2, '14:00', '18:00', 'datos-de-prueba', TRUE),
    (5, 1, 3, '09:00', '13:00', 'datos-de-prueba', TRUE),
    (6, 1, 3, '14:00', '18:00', 'datos-de-prueba', TRUE),
    (7, 1, 4, '09:00', '13:00', 'datos-de-prueba', TRUE),
    (8, 1, 4, '14:00', '18:00', 'datos-de-prueba', TRUE),
    (9, 1, 5, '09:00', '13:00', 'datos-de-prueba', TRUE),
    -- Martín Sosa: martes y jueves corrido, sábado a la mañana
    (10, 2, 2, '10:00', '19:00', 'datos-de-prueba', TRUE),
    (11, 2, 4, '10:00', '19:00', 'datos-de-prueba', TRUE),
    (12, 2, 6, '09:00', '13:00', 'datos-de-prueba', TRUE),
    -- Carolina Ruiz: lunes, miércoles y viernes a la mañana
    (13, 3, 1, '08:00', '14:00', 'datos-de-prueba', TRUE),
    (14, 3, 3, '08:00', '14:00', 'datos-de-prueba', TRUE),
    (15, 3, 5, '08:00', '14:00', 'datos-de-prueba', TRUE),
    -- Diego Fernández: lunes a viernes a la tarde
    (16, 4, 1, '14:00', '20:00', 'datos-de-prueba', TRUE),
    (17, 4, 2, '14:00', '20:00', 'datos-de-prueba', TRUE),
    (18, 4, 3, '14:00', '20:00', 'datos-de-prueba', TRUE),
    (19, 4, 4, '14:00', '20:00', 'datos-de-prueba', TRUE),
    (20, 4, 5, '14:00', '20:00', 'datos-de-prueba', TRUE),
    -- Ana Paz (profesional inactiva) y un horario dado de baja
    (21, 5, 1, '09:00', '17:00', 'datos-de-prueba', FALSE),
    (22, 1, 5, '14:00', '18:00', 'datos-de-prueba', FALSE);

-- -------------------------------------------------------------------------------------
-- 5. Feriados nacionales (Argentina). Verificar traslados oficiales antes de usar en real.
-- -------------------------------------------------------------------------------------
INSERT INTO feriados (fecha, descripcion, aplica_a_todos, created_by, activo) VALUES
    ('2026-10-12', 'Día del Respeto a la Diversidad Cultural', TRUE, 'datos-de-prueba', TRUE),
    ('2026-11-23', 'Día de la Soberanía Nacional (trasladado)', TRUE, 'datos-de-prueba', TRUE),
    ('2026-12-08', 'Inmaculada Concepción de María', TRUE, 'datos-de-prueba', TRUE),
    ('2026-12-24', 'Nochebuena (cierre del consultorio)', FALSE, 'datos-de-prueba', TRUE),
    ('2026-12-25', 'Navidad', TRUE, 'datos-de-prueba', TRUE),
    ('2026-12-31', 'Fin de año (cierre del consultorio)', FALSE, 'datos-de-prueba', TRUE),
    ('2027-01-01', 'Año Nuevo', TRUE, 'datos-de-prueba', TRUE),
    ('2027-02-08', 'Carnaval', TRUE, 'datos-de-prueba', TRUE),
    ('2027-02-09', 'Carnaval', TRUE, 'datos-de-prueba', TRUE),
    ('2027-03-24', 'Día Nacional de la Memoria por la Verdad y la Justicia', TRUE, 'datos-de-prueba', TRUE),
    ('2027-03-26', 'Viernes Santo', TRUE, 'datos-de-prueba', TRUE),
    ('2027-04-02', 'Día del Veterano y de los Caídos en Malvinas', TRUE, 'datos-de-prueba', TRUE),
    ('2027-05-01', 'Día del Trabajador', TRUE, 'datos-de-prueba', TRUE),
    ('2027-05-25', 'Día de la Revolución de Mayo', TRUE, 'datos-de-prueba', TRUE),
    ('2027-06-20', 'Paso a la Inmortalidad del Gral. Manuel Belgrano', TRUE, 'datos-de-prueba', TRUE),
    ('2027-07-09', 'Día de la Independencia', TRUE, 'datos-de-prueba', TRUE),
    ('2027-03-25', 'Jueves Santo (dado de baja: no laborable optativo)', FALSE, 'datos-de-prueba', FALSE);

-- -------------------------------------------------------------------------------------
-- 6. Usuarios de staff (una cuenta por rol). Contraseña de todos: Prueba1234!
-- -------------------------------------------------------------------------------------
INSERT INTO usuarios (id, rol_id, email, hash_password, proveedor, created_by, activo) VALUES
    (1, 4, 'admin@turnos-odontologia.test',      '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (2, 2, 'recepcion@turnos-odontologia.test',  '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (3, 2, 'recepcion2@turnos-odontologia.test', '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (4, 3, 'lgimenez@turnos-odontologia.test',   '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (5, 3, 'msosa@turnos-odontologia.test',      '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (6, 3, 'cruiz@turnos-odontologia.test',      '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (7, 3, 'dfernandez@turnos-odontologia.test', '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (8, 5, 'auditor@turnos-odontologia.test',    '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (9, 1, 'paciente@turnos-odontologia.test',   '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', TRUE),
    (10, 2, 'baja@turnos-odontologia.test',      '$2a$10$AUTrSVt.aa08M/IhPJu1xOyl98ikjTLmBlsfg11UQcosj9ksBWi7O', 'LOCAL', 'datos-de-prueba', FALSE);

-- Vínculo usuario odontólogo -> profesional (V033), para el ámbito «propio» de agenda y ficha
UPDATE usuarios SET profesional_id = v.prof
FROM (VALUES (4, 1), (5, 2), (6, 3), (7, 4)) AS v(usuario, prof)
WHERE usuarios.id = v.usuario;

-- -------------------------------------------------------------------------------------
-- 7. Pacientes ficticios (adultos, menores para dientes temporales, sin consentimiento,
--    sin email, CUIT, pasaporte, dado de baja y documento reutilizado tras la baja)
-- -------------------------------------------------------------------------------------
INSERT INTO pacientes (id, documento_tipo, documento_numero, nombre, apellido, telefono, fecha_nacimiento,
                       email, direccion, consentimiento_datos, fecha_consentimiento, version_politica,
                       finalidad, canal, created_by, activo) VALUES
    (1,  'DNI', '90000001', 'Sofía',     'Martínez',  '+5491150000001', '1988-03-14', 'sofia.martinez@ejemplo.test',  'Av. Siempre Viva 101, CABA',     TRUE,  now() - interval '200 days', 'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (2,  'DNI', '90000002', 'Juan',      'Pérez',     '+5491150000002', '1975-07-02', 'juan.perez@ejemplo.test',      'Calle Falsa 123, CABA',          TRUE,  now() - interval '190 days', 'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (3,  'DNI', '90000003', 'Valentina', 'López',     '+5491150000003', '1995-11-23', 'valen.lopez@ejemplo.test',     'Mitre 450, Avellaneda',          TRUE,  now() - interval '180 days', 'v1.0', 'atención odontológica y recordatorios', 'whatsapp',  'datos-de-prueba', TRUE),
    (4,  'DNI', '90000004', 'Mateo',     'González',  '+5491150000004', '2017-05-09', NULL,                           'Belgrano 900, Quilmes',          TRUE,  now() - interval '170 days', 'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (5,  'DNI', '90000005', 'Emma',      'Rodríguez', '+5491150000005', '2019-08-30', NULL,                           'Belgrano 900, Quilmes',          TRUE,  now() - interval '170 days', 'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (6,  'DNI', '90000006', 'Lucas',     'Fernández', '+5491150000006', '1982-01-17', 'lucas.f@ejemplo.test',         'San Martín 77, Lanús',           TRUE,  now() - interval '160 days', 'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (7,  'DNI', '90000007', 'Martina',   'García',    '+5491150000007', '2000-06-05', 'martina.garcia@ejemplo.test',  'Rivadavia 5000, CABA',           TRUE,  now() - interval '150 days', 'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (8,  'DNI', '90000008', 'Benjamín',  'Romero',    '+5491150000008', '1968-09-12', 'benja.romero@ejemplo.test',    'Corrientes 3200, CABA',          TRUE,  now() - interval '140 days', 'v1.0', 'atención odontológica y recordatorios', 'telefono',  'datos-de-prueba', TRUE),
    (9,  'DNI', '90000009', 'Isabella',  'Sánchez',   '+5491150000009', '2015-02-27', NULL,                           'Lavalle 1500, CABA',             TRUE,  now() - interval '130 days', 'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (10, 'DNI', '90000010', 'Thiago',    'Díaz',      '+5491150000010', '1990-12-01', 'thiago.diaz@ejemplo.test',     'Santa Fe 2100, CABA',            TRUE,  now() - interval '120 days', 'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (11, 'DNI', '90000011', 'Camila',    'Torres',    '+5491150000011', '1985-04-19', 'camila.torres@ejemplo.test',   'Callao 800, CABA',               TRUE,  now() - interval '115 days', 'v1.0', 'atención odontológica y recordatorios', 'whatsapp',  'datos-de-prueba', TRUE),
    (12, 'DNI', '90000012', 'Santiago',  'Álvarez',   '+5491150000012', '1979-10-08', 'santi.alvarez@ejemplo.test',   'Cabildo 2500, CABA',             TRUE,  now() - interval '110 days', 'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (13, 'DNI', '90000013', 'Mía',       'Ruiz',      '+5491150000013', '2018-07-21', NULL,                           'Cabildo 2500, CABA',             TRUE,  now() - interval '110 days', 'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (14, 'DNI', '90000014', 'Joaquín',   'Moreno',    '+5491150000014', '1999-03-03', 'joaquin.moreno@ejemplo.test',  'Juramento 1900, CABA',           TRUE,  now() - interval '100 days', 'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (15, 'DNI', '90000015', 'Lucía',     'Muñoz',     '+5491150000015', '1972-08-15', 'lucia.munoz@ejemplo.test',     'Pueyrredón 1100, CABA',          TRUE,  now() - interval '95 days',  'v1.0', 'atención odontológica y recordatorios', 'telefono',  'datos-de-prueba', TRUE),
    (16, 'DNI', '90000016', 'Felipe',    'Rojas',     '+5491150000016', '2005-01-29', 'felipe.rojas@ejemplo.test',    'Acoyte 300, CABA',               TRUE,  now() - interval '90 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (17, 'DNI', '90000017', 'Catalina',  'Benítez',   '+5491150000017', '1993-05-25', 'cata.benitez@ejemplo.test',    'Directorio 4000, CABA',          TRUE,  now() - interval '85 days',  'v1.0', 'atención odontológica y recordatorios', 'whatsapp',  'datos-de-prueba', TRUE),
    (18, 'DNI', '90000018', 'Agustín',   'Medina',    '+5491150000018', '1960-11-11', NULL,                           'Gaona 2800, CABA',               TRUE,  now() - interval '80 days',  'v1.0', 'atención odontológica y recordatorios', 'telefono',  'datos-de-prueba', TRUE),
    (19, 'DNI', '90000019', 'Olivia',    'Herrera',   '+5491150000019', '2020-09-14', NULL,                           'Gaona 2800, CABA',               TRUE,  now() - interval '80 days',  'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (20, 'DNI', '90000020', 'Bautista',  'Aguirre',   '+5491150000020', '1987-02-06', 'bauti.aguirre@ejemplo.test',   'Nazca 1500, CABA',               TRUE,  now() - interval '75 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (21, 'DNI', '90000021', 'Julieta',   'Pereyra',   '+5491150000021', '1998-12-19', 'juli.pereyra@ejemplo.test',    'Boedo 700, CABA',                TRUE,  now() - interval '70 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (22, 'DNI', '90000022', 'Tomás',     'Gutiérrez', '+5491150000022', '1983-06-30', 'tomas.gutierrez@ejemplo.test', 'Independencia 2200, CABA',       TRUE,  now() - interval '65 days',  'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (23, 'DNI', '90000023', 'Renata',    'Giménez',   '+5491150000023', '2016-04-04', NULL,                           'Independencia 2200, CABA',       TRUE,  now() - interval '65 days',  'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (24, 'DNI', '90000024', 'Facundo',   'Castro',    '+5491150000024', '1970-10-27', 'facu.castro@ejemplo.test',     'Entre Ríos 1300, CABA',          TRUE,  now() - interval '60 days',  'v1.0', 'atención odontológica y recordatorios', 'telefono',  'datos-de-prueba', TRUE),
    (25, 'DNI', '90000025', 'Abril',     'Vargas',    '+5491150000025', '2002-03-16', 'abril.vargas@ejemplo.test',    'Jujuy 600, CABA',                TRUE,  now() - interval '55 days',  'v1.0', 'atención odontológica y recordatorios', 'whatsapp',  'datos-de-prueba', TRUE),
    (26, 'DNI', '90000026', 'Ignacio',   'Ortiz',     '+5491150000026', '1991-07-07', 'nacho.ortiz@ejemplo.test',     'Alsina 1000, CABA',              TRUE,  now() - interval '50 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (27, 'DNI', '90000027', 'Victoria',  'Silva',     '+5491150000027', '1977-01-09', 'vicky.silva@ejemplo.test',     'Perú 500, CABA',                 TRUE,  now() - interval '45 days',  'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (28, 'DNI', '90000028', 'Nicolás',   'Núñez',     '+5491150000028', '1996-09-21', 'nico.nunez@ejemplo.test',      'Defensa 800, CABA',              TRUE,  now() - interval '40 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (29, 'DNI', '90000029', 'Guadalupe', 'Luna',      '+5491150000029', '1989-05-13', 'guada.luna@ejemplo.test',      'Bolívar 300, CABA',              TRUE,  now() - interval '35 days',  'v1.0', 'atención odontológica y recordatorios', 'whatsapp',  'datos-de-prueba', TRUE),
    (30, 'DNI', '90000030', 'Lautaro',   'Cabrera',   '+5491150000030', '1965-12-24', NULL,                           'Chile 900, CABA',                TRUE,  now() - interval '30 days',  'v1.0', 'atención odontológica y recordatorios', 'telefono',  'datos-de-prueba', TRUE),
    (31, 'CUIT', '20900000319', 'Paula',  'Ríos',      '+5491150000031', '1984-08-08', 'paula.rios@ejemplo.test',      'Tacuarí 400, CABA',              TRUE,  now() - interval '28 days',  'v1.0', 'atención odontológica, recordatorios y facturación', 'recepcion', 'datos-de-prueba', TRUE),
    (32, 'CUIT', '27900000325', 'Germán', 'Molina',    '+5491150000032', '1976-02-14', 'german.molina@ejemplo.test',   'Piedras 1200, CABA',             TRUE,  now() - interval '25 days',  'v1.0', 'atención odontológica, recordatorios y facturación', 'link',      'datos-de-prueba', TRUE),
    (33, 'PASAPORTE', 'PRUEBA-AB123456', 'Lucas', 'Schmidt', '+4915100000033', '1990-04-01', 'l.schmidt@ejemplo.test', 'Hotel en Palermo, CABA',        TRUE,  now() - interval '20 days',  'v1.0', 'atención odontológica y recordatorios', 'recepcion', 'datos-de-prueba', TRUE),
    (34, 'PASAPORTE', 'PRUEBA-UY998877', 'Florencia', 'Acosta', '+59899000034', '1994-10-10', 'flor.acosta@ejemplo.test', 'Montevideo (turista)',           TRUE,  now() - interval '18 days',  'v1.0', 'atención odontológica y recordatorios', 'whatsapp',  'datos-de-prueba', TRUE),
    (35, 'DNI', '90000035', 'Ramiro',    'Ibáñez',    '+5491150000035', '1981-03-28', 'ramiro.ibanez@ejemplo.test',   'Humberto Primo 600, CABA',       TRUE,  now() - interval '15 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (36, 'DNI', '90000036', 'Milagros',  'Correa',    '+5491150000036', '2001-11-02', 'mili.correa@ejemplo.test',     'Venezuela 1100, CABA',           TRUE,  now() - interval '12 days',  'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    (37, 'DNI', '90000037', 'Emiliano',  'Quiroga',   '+5491150000037', '1973-06-18', 'emi.quiroga@ejemplo.test',     'México 2000, CABA',              TRUE,  now() - interval '10 days',  'v1.0', 'atención odontológica y recordatorios', 'telefono',  'datos-de-prueba', TRUE),
    (38, 'DNI', '90000038', 'Delfina',   'Ferreyra',  '+5491150000038', '1997-01-31', 'delfi.ferreyra@ejemplo.test',  'Estados Unidos 900, CABA',       TRUE,  now() - interval '8 days',   'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    -- Sin consentimiento de datos registrado (Ley 25.326): para probar el bloqueo/flujo de consentimiento
    (39, 'DNI', '90000039', 'Hernán',    'Villalba',  '+5491150000039', '1986-07-27', NULL,                           NULL,                             FALSE, NULL, NULL, NULL, NULL,              'datos-de-prueba', TRUE),
    -- Paciente dado de baja y paciente nuevo con el mismo documento (índice único parcial)
    (40, 'DNI', '90000040', 'Rocío',     'Paredes',   '+5491150000040', '1992-02-22', 'rocio.vieja@ejemplo.test',     'Dirección anterior',             TRUE,  now() - interval '400 days', 'v0.9', 'atención odontológica', 'recepcion', 'datos-de-prueba', FALSE),
    (41, 'DNI', '90000040', 'Rocío',     'Paredes',   '+5491150000041', '1992-02-22', 'rocio.paredes@ejemplo.test',   'Dirección actual 55, CABA',      TRUE,  now() - interval '5 days',   'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE),
    -- Paciente nuevo, sin turnos ni ficha: para probar la primera reserva por link
    (42, 'DNI', '90000042', 'Valentín',  'Navarro',   '+5491150000042', '1999-09-09', 'valentin.navarro@ejemplo.test','Rondeau 300, CABA',              TRUE,  now() - interval '1 day',    'v1.0', 'atención odontológica y recordatorios', 'link',      'datos-de-prueba', TRUE);

-- -------------------------------------------------------------------------------------
-- 8. Agenda: bloqueos, turnos (pasados, hoy y futuros), sobreturnos y ocupaciones.
--    Se genera con un bloque procedural para respetar horarios, feriados, días hábiles
--    y la restricción de exclusión de solapamiento por profesional y por sillón.
-- -------------------------------------------------------------------------------------
CREATE FUNCTION pg_temp.dia_habil(n INTEGER) RETURNS DATE AS $$
DECLARE
    d DATE := current_date;
    paso INTEGER := CASE WHEN n < 0 THEN -1 ELSE 1 END;
    restantes INTEGER := abs(n);
BEGIN
    -- n = 0: hoy si es hábil, si no el próximo hábil
    WHILE EXTRACT(ISODOW FROM d) > 5 OR EXISTS (SELECT 1 FROM feriados f WHERE f.fecha = d AND f.activo) LOOP
        d := d + 1;
    END LOOP;
    WHILE restantes > 0 LOOP
        d := d + paso;
        IF EXTRACT(ISODOW FROM d) <= 5 AND NOT EXISTS (SELECT 1 FROM feriados f WHERE f.fecha = d AND f.activo) THEN
            restantes := restantes - 1;
        END IF;
    END LOOP;
    RETURN d;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.ts(d DATE, h TIME) RETURNS TIMESTAMPTZ AS $$
    SELECT (d + h) AT TIME ZONE 'America/Argentina/Buenos_Aires';
$$ LANGUAGE sql;

-- Bloqueos (vacaciones, capacitación, mantenimiento de sillón, trámite; uno anulado)
INSERT INTO bloqueos (id, profesional_id, sillon_id, desde, hasta, motivo, created_by, activo) VALUES
    (1, 1, NULL, pg_temp.ts(pg_temp.dia_habil(8), '00:00'), pg_temp.ts(pg_temp.dia_habil(9), '23:59'),
        'Vacaciones Dra. Giménez', 'recepcion@turnos-odontologia.test', TRUE),
    (2, NULL, 2, pg_temp.ts(pg_temp.dia_habil(3), '08:00'), pg_temp.ts(pg_temp.dia_habil(3), '13:00'),
        'Mantenimiento preventivo Sillón 2', 'admin@turnos-odontologia.test', TRUE),
    (3, 3, NULL, pg_temp.ts(pg_temp.dia_habil(-5), '08:00'), pg_temp.ts(pg_temp.dia_habil(-5), '11:00'),
        'Capacitación en odontopediatría', 'recepcion@turnos-odontologia.test', TRUE),
    (4, 4, NULL, pg_temp.ts(pg_temp.dia_habil(1), '14:00'), pg_temp.ts(pg_temp.dia_habil(1), '16:00'),
        'Trámite personal', 'recepcion2@turnos-odontologia.test', TRUE),
    (5, 2, 2, pg_temp.ts(pg_temp.dia_habil(4), '10:00'), pg_temp.ts(pg_temp.dia_habil(4), '12:00'),
        'Congreso de ortodoncia (anulado)', 'recepcion@turnos-odontologia.test', FALSE),
    (6, NULL, 4, pg_temp.ts(pg_temp.dia_habil(-10), '00:00'), pg_temp.ts(pg_temp.dia_habil(15), '23:59'),
        'Sillón 4 fuera de servicio (reparación del equipo)', 'admin@turnos-odontologia.test', TRUE);

INSERT INTO ocupaciones (bloqueo_id, profesional_id, sillon_id, inicio, fin, activo)
SELECT id, profesional_id, sillon_id, desde, hasta, activo FROM bloqueos;

DO $$
DECLARE
    -- profesional -> sillón habitual
    sillon_de INTEGER[] := ARRAY[1, 2, 3, 3];
    -- tratamientos que rota cada profesional
    trat_de INTEGER[][] := ARRAY[
        ARRAY[1, 2, 3, 5, 8, 11],     -- general
        ARRAY[6, 6, 7, 1, 6, 6],      -- ortodoncia
        ARRAY[9, 10, 9, 2, 3, 9],     -- odontopediatría
        ARRAY[4, 1, 4, 3, 12, 4]];    -- endodoncia
    -- pacientes por especialidad (los menores van a odontopediatría)
    pac_adultos INTEGER[] := ARRAY[1,2,3,6,7,8,10,11,12,14,15,16,17,18,20,21,22,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,41];
    pac_menores INTEGER[] := ARRAY[4,5,9,13,19,23];
    origenes TEXT[] := ARRAY['agenda', 'link', 'recepcion', 'telefono', 'link'];
    n INTEGER;
    dia DATE;
    h RECORD;
    slot TIMESTAMPTZ;
    fin_slot TIMESTAMPTZ;
    tid INTEGER;
    dur INTEGER;
    sil INTEGER;
    pac INTEGER;
    es_activo BOOLEAN;
    est TEXT;
    semilla INTEGER;
    contador INTEGER := 0;
    nuevo_id BIGINT;
BEGIN
    FOR n IN -20..15 LOOP
        dia := pg_temp.dia_habil(n);
        FOR h IN SELECT * FROM horarios
                 WHERE horarios.activo AND horarios.profesional_id IN (1, 2, 3, 4)
                   AND horarios.dia_semana = EXTRACT(ISODOW FROM dia)
                 ORDER BY horarios.profesional_id, horarios.desde LOOP
            slot := pg_temp.ts(dia, h.desde);
            WHILE TRUE LOOP
                contador := contador + 1;
                semilla := (abs(n) * 31 + h.profesional_id * 7 + contador * 13) % 20;
                tid := trat_de[h.profesional_id][1 + (contador % 6)];
                SELECT duracion_minutos INTO dur FROM tratamientos WHERE id = tid;
                fin_slot := slot + make_interval(mins => dur);
                EXIT WHEN fin_slot > pg_temp.ts(dia, h.hasta);

                -- Huecos libres: ~25% en el pasado, ~45% en el futuro (para probar disponibilidad)
                IF (n < 0 AND semilla < 5) OR (n >= 0 AND semilla < 9) THEN
                    slot := fin_slot + interval '15 minutes';
                    CONTINUE;
                END IF;

                sil := sillon_de[h.profesional_id];
                IF EXISTS (SELECT 1 FROM ocupaciones o
                           WHERE o.activo AND tstzrange(slot, fin_slot, '[)') && o.rango
                             AND (o.profesional_id = h.profesional_id OR o.sillon_id = sil)) THEN
                    slot := fin_slot;
                    CONTINUE;
                END IF;

                IF h.profesional_id = 3 THEN
                    pac := pac_menores[1 + (contador % array_length(pac_menores, 1))];
                ELSE
                    pac := pac_adultos[1 + ((contador * 7 + n + 40) % array_length(pac_adultos, 1))];
                END IF;

                IF n < 0 THEN
                    est := CASE WHEN semilla IN (5, 6) THEN 'inasistente'
                                   WHEN semilla IN (7, 8) THEN 'cancelado'
                                   ELSE 'confirmado' END;
                ELSE
                    est := CASE WHEN semilla IN (9, 10) THEN 'cancelado'
                                   WHEN semilla BETWEEN 11 AND 15 THEN 'confirmado'
                                   ELSE 'reservado' END;
                END IF;
                es_activo := est <> 'cancelado';

                INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, inicio, fin,
                                    duracion_minutos, estado, origen, es_sobreturno, created_at, created_by,
                                    updated_at, updated_by, activo)
                VALUES (pac, h.profesional_id, sil, tid, slot, fin_slot, dur, est,
                        origenes[1 + (contador % 5)], FALSE,
                        LEAST(slot, now()) - interval '7 days', 'datos-de-prueba',
                        CASE WHEN est IN ('confirmado', 'cancelado', 'inasistente')
                             THEN LEAST(slot, now()) - interval '1 day' END,
                        CASE WHEN est IN ('confirmado', 'cancelado', 'inasistente')
                             THEN 'datos-de-prueba' END,
                        es_activo)
                RETURNING id INTO nuevo_id;

                INSERT INTO ocupaciones (turno_id, profesional_id, sillon_id, inicio, fin, activo)
                VALUES (nuevo_id, h.profesional_id, sil, slot, fin_slot, es_activo);

                slot := fin_slot;
            END LOOP;
        END LOOP;
    END LOOP;
END;
$$;

-- Sobreturnos: fuera del horario de atención, con motivo obligatorio (RN-AG-02)
DO $$
DECLARE
    s RECORD;
    nuevo_id BIGINT;
BEGIN
    FOR s IN SELECT * FROM (VALUES
            (1, 1, 12, 2,  pg_temp.ts(pg_temp.dia_habil(2), '18:00'),  'Urgencia: dolor agudo en pieza 36', 'confirmado'),
            (4, 3, 12, 18, pg_temp.ts(pg_temp.dia_habil(-3), '20:00'), 'Urgencia: absceso, derivado por guardia', 'confirmado'),
            (1, 1, 1,  14, pg_temp.ts(pg_temp.dia_habil(5), '13:00'),  'Paciente viaja: única ventana disponible', 'reservado')
        ) AS v(prof, sil, trat, pac, inicio, motivo, estado) LOOP
        INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, inicio, fin,
                            duracion_minutos, estado, origen, es_sobreturno, motivo_override, created_by, activo)
        SELECT s.pac, s.prof, s.sil, s.trat, s.inicio, s.inicio + make_interval(mins => t.duracion_minutos),
               t.duracion_minutos, s.estado, 'recepcion', TRUE, s.motivo, 'recepcion@turnos-odontologia.test', TRUE
        FROM tratamientos t WHERE t.id = s.trat
          AND NOT EXISTS (SELECT 1 FROM ocupaciones o WHERE o.activo
                          AND o.rango && tstzrange(s.inicio, s.inicio + make_interval(mins => t.duracion_minutos), '[)')
                          AND (o.profesional_id = s.prof OR o.sillon_id = s.sil))
        RETURNING id INTO nuevo_id;
        IF nuevo_id IS NOT NULL THEN
            INSERT INTO ocupaciones (turno_id, profesional_id, sillon_id, inicio, fin, activo)
            SELECT id, profesional_id, sillon_id, inicio, fin, TRUE FROM turnos WHERE id = nuevo_id;
        END IF;
        nuevo_id := NULL;
    END LOOP;
END;
$$;

-- Turnos específicos del paciente con usuario (paciente@...): Sofía Martínez (id 1), para
-- probar la ventana de 48 hs hábiles: uno mañana (dentro de la ventana) y otro a 6 días hábiles.
DO $$
DECLARE
    s RECORD;
    nuevo_id BIGINT;
BEGIN
    FOR s IN SELECT * FROM (VALUES
            (1, 1, 2, pg_temp.ts(pg_temp.dia_habil(1), '11:00'), 'dentro de 48 hs hábiles'),
            (1, 1, 3, pg_temp.ts(pg_temp.dia_habil(6), '09:00'), 'fuera de 48 hs hábiles')
        ) AS v(prof, sil, trat, inicio, nota) LOOP
        UPDATE ocupaciones o SET activo = FALSE
        FROM turnos t
        WHERE o.turno_id = t.id AND o.activo
          AND o.rango && tstzrange(s.inicio, s.inicio + interval '90 minutes', '[)')
          AND (o.profesional_id = s.prof OR o.sillon_id = s.sil);
        UPDATE turnos t SET estado = 'cancelado', activo = FALSE, updated_at = now(),
                            updated_by = 'datos-de-prueba'
        WHERE t.activo AND t.rango && tstzrange(s.inicio, s.inicio + interval '90 minutes', '[)')
          AND (t.profesional_id = s.prof OR t.sillon_id = s.sil);

        INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, inicio, fin,
                            duracion_minutos, estado, origen, es_sobreturno, created_by, activo)
        SELECT 1, s.prof, s.sil, t.id, s.inicio, s.inicio + make_interval(mins => t.duracion_minutos),
               t.duracion_minutos, 'reservado', 'link', FALSE, 'datos-de-prueba (' || s.nota || ')', TRUE
        FROM tratamientos t WHERE t.id = s.trat
        RETURNING id INTO nuevo_id;
        INSERT INTO ocupaciones (turno_id, profesional_id, sillon_id, inicio, fin, activo)
        SELECT id, profesional_id, sillon_id, inicio, fin, TRUE FROM turnos WHERE id = nuevo_id;
    END LOOP;
END;
$$;

-- -------------------------------------------------------------------------------------
-- 9. WhatsApp: plantillas, notificaciones en todos los estados, outbox, intentos, webhooks
-- -------------------------------------------------------------------------------------
INSERT INTO plantillas_whatsapp (id, codigo_meta, proposito, idioma, estado_aprobacion, motivo_rechazo, activo) VALUES
    (1, 'turno_recordatorio_v0',   'RECORDATORIO', 'es_AR', 'RECHAZADA', 'Contenido promocional no permitido en categoría UTILITY', TRUE),
    (2, 'turno_confirmacion_v1',   'CONFIRMACION', 'es_AR', 'APROBADA',  NULL, TRUE),
    (3, 'turno_recordatorio_v1',   'RECORDATORIO', 'es_AR', 'APROBADA',  NULL, TRUE),
    (4, 'turno_confirmacion_v2',   'CONFIRMACION', 'es_AR', 'PENDIENTE', NULL, TRUE),
    (5, 'turno_recordatorio_en',   'RECORDATORIO', 'en_US', 'APROBADA',  NULL, FALSE);

DO $$
DECLARE
    t RECORD;
    i INTEGER := 0;
    est TEXT;
    nid BIGINT;
    wamid TEXT;
    prog TIMESTAMPTZ;
BEGIN
    -- Futuros activos: confirmación + recordatorio 24 h antes
    FOR t IN SELECT tu.id, tu.inicio, tu.created_at, p.telefono
             FROM turnos tu JOIN pacientes p ON p.id = tu.paciente_id
             WHERE tu.activo AND tu.inicio > now()
             ORDER BY tu.inicio LIMIT 40 LOOP
        i := i + 1;
        wamid := 'wamid.PRUEBA-C-' || t.id;
        est := (ARRAY['ENVIADA', 'ENTREGADA', 'LEIDA', 'CONFIRMADA', 'ENTREGADA', 'FALLIDA', 'PENDIENTE', 'LEIDA'])[1 + (i % 8)];
        prog := t.created_at + interval '1 minute';
        INSERT INTO notificaciones (turno_id, plantilla_id, proposito, destinatario, estado, intentos,
                                    proveedor_message_id, programada_para, idempotency_key, ultimo_error, created_at, updated_at)
        VALUES (t.id, 2, 'CONFIRMACION', t.telefono, est,
                CASE est WHEN 'PENDIENTE' THEN 0 WHEN 'FALLIDA' THEN 3 ELSE 1 END,
                CASE WHEN est IN ('PENDIENTE', 'FALLIDA') THEN NULL ELSE wamid END,
                prog, t.id || ':CONFIRMACION:' || (extract(epoch FROM prog) * 1000)::BIGINT,
                CASE WHEN est = 'FALLIDA' THEN 'proveedor: número no registrado en WhatsApp (131026)' END,
                prog, CASE WHEN est <> 'PENDIENTE' THEN prog + interval '2 minutes' END)
        RETURNING id INTO nid;

        IF est = 'PENDIENTE' THEN
            INSERT INTO notificaciones_outbox (notificacion_id, estado, proximo_intento) VALUES (nid, 'PENDIENTE', now());
        ELSE
            INSERT INTO notificaciones_outbox (notificacion_id, estado, proximo_intento, procesado_at)
            VALUES (nid, CASE WHEN est = 'FALLIDA' THEN 'PROCESADO' ELSE 'PROCESADO' END, prog, prog + interval '1 minute');
        END IF;

        IF est = 'FALLIDA' THEN
            INSERT INTO notificacion_intentos (notificacion_id, numero, fecha, resultado, error) VALUES
                (nid, 1, prog, 'ERROR', 'proveedor: número no registrado en WhatsApp (131026)'),
                (nid, 2, prog + interval '5 minutes', 'ERROR', 'proveedor: número no registrado en WhatsApp (131026)'),
                (nid, 3, prog + interval '15 minutes', 'ERROR', 'proveedor: número no registrado en WhatsApp (131026)');
        ELSIF est <> 'PENDIENTE' THEN
            INSERT INTO notificacion_intentos (notificacion_id, numero, fecha, resultado, proveedor_message_id)
            VALUES (nid, 1, prog, 'EXITO', wamid);
            IF est IN ('ENTREGADA', 'LEIDA', 'CONFIRMADA') THEN
                INSERT INTO whatsapp_webhook_eventos (evento_id, tipo, proveedor_message_id, recibido_at)
                VALUES ('evt-prueba-' || nid || '-delivered', 'delivered', wamid, prog + interval '3 minutes');
            END IF;
            IF est IN ('LEIDA', 'CONFIRMADA') THEN
                INSERT INTO whatsapp_webhook_eventos (evento_id, tipo, proveedor_message_id, recibido_at)
                VALUES ('evt-prueba-' || nid || '-read', 'read', wamid, prog + interval '10 minutes');
            END IF;
            IF est = 'CONFIRMADA' THEN
                INSERT INTO whatsapp_webhook_eventos (evento_id, tipo, proveedor_message_id, recibido_at)
                VALUES ('evt-prueba-' || nid || '-reply', 'button_reply', wamid, prog + interval '12 minutes');
            END IF;
        END IF;

        -- Recordatorio 24 h antes: pendiente en outbox
        prog := t.inicio - interval '24 hours';
        INSERT INTO notificaciones (turno_id, plantilla_id, proposito, destinatario, estado, intentos,
                                    programada_para, idempotency_key, created_at)
        VALUES (t.id, 3, 'RECORDATORIO', t.telefono, 'PENDIENTE', 0, prog,
                t.id || ':RECORDATORIO:' || (extract(epoch FROM prog) * 1000)::BIGINT, t.created_at)
        RETURNING id INTO nid;
        INSERT INTO notificaciones_outbox (notificacion_id, estado, proximo_intento) VALUES (nid, 'PENDIENTE', prog);
    END LOOP;

    -- Pasados: recordatorios ya enviados y confirmados (alimenta la métrica de confirmación)
    i := 0;
    FOR t IN SELECT tu.id, tu.inicio, tu.estado, p.telefono
             FROM turnos tu JOIN pacientes p ON p.id = tu.paciente_id
             WHERE tu.estado IN ('confirmado', 'inasistente') AND tu.inicio < now()
             ORDER BY tu.inicio LIMIT 60 LOOP
        i := i + 1;
        wamid := 'wamid.PRUEBA-R-' || t.id;
        prog := t.inicio - interval '24 hours';
        est := CASE WHEN t.estado = 'inasistente' THEN 'LEIDA'
                    WHEN i % 4 = 0 THEN 'ENTREGADA' ELSE 'CONFIRMADA' END;
        INSERT INTO notificaciones (turno_id, plantilla_id, proposito, destinatario, estado, intentos,
                                    proveedor_message_id, programada_para, idempotency_key, created_at, updated_at)
        VALUES (t.id, 3, 'RECORDATORIO', t.telefono, est, 1, wamid, prog,
                t.id || ':RECORDATORIO:' || (extract(epoch FROM prog) * 1000)::BIGINT, prog - interval '2 days', prog + interval '1 hour')
        RETURNING id INTO nid;
        INSERT INTO notificaciones_outbox (notificacion_id, estado, proximo_intento, procesado_at)
        VALUES (nid, 'PROCESADO', prog, prog + interval '1 minute');
        INSERT INTO notificacion_intentos (notificacion_id, numero, fecha, resultado, proveedor_message_id)
        VALUES (nid, 1, prog, 'EXITO', wamid);
        INSERT INTO whatsapp_webhook_eventos (evento_id, tipo, proveedor_message_id, recibido_at)
        VALUES ('evt-prueba-' || nid || '-delivered', 'delivered', wamid, prog + interval '2 minutes');
        IF est IN ('LEIDA', 'CONFIRMADA') THEN
            INSERT INTO whatsapp_webhook_eventos (evento_id, tipo, proveedor_message_id, recibido_at)
            VALUES ('evt-prueba-' || nid || '-read', 'read', wamid, prog + interval '20 minutes');
        END IF;
    END LOOP;
END;
$$;

-- Notificación BLOQUEADA (plantilla rechazada por Meta) con aviso a administración
INSERT INTO notificaciones (turno_id, plantilla_id, proposito, destinatario, estado, intentos, programada_para,
                            idempotency_key, ultimo_error)
SELECT tu.id, 1, 'RECORDATORIO', p.telefono, 'BLOQUEADA', 0, tu.inicio - interval '48 hours',
       tu.id || ':RECORDATORIO:BLOQUEADA-PRUEBA', 'plantilla-no-aprobada'
FROM turnos tu JOIN pacientes p ON p.id = tu.paciente_id
WHERE tu.activo AND tu.inicio > now() ORDER BY tu.inicio DESC LIMIT 1;

-- Evento de webhook duplicado no se repite (idempotencia): ver evento_id único.

-- -------------------------------------------------------------------------------------
-- 10. Historia clínica: fichas, anamnesis versionada, odontograma FDI (permanentes y
--     temporales), evoluciones (una reemplazada por versión 2), consentimientos y firmas
-- -------------------------------------------------------------------------------------
INSERT INTO fichas_clinicas (id, paciente_id, turno_apertura_id, fecha_apertura)
SELECT row_number() OVER (ORDER BY p.id), p.id,
       (SELECT min(t.id) FROM turnos t WHERE t.paciente_id = p.id AND t.estado = 'confirmado' AND t.inicio < now()),
       now() - interval '90 days' + (p.id || ' hours')::interval
FROM pacientes p
WHERE p.id IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,17,18,19,22,23,24,30,31,33);

-- Anamnesis versión 1 para todas las fichas; versión 2 para algunas (actualización)
INSERT INTO anamnesis (ficha_id, version, respuestas, texto_libre, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, 1,
       jsonb_build_object(
           'alergias', (ARRAY['ninguna', 'penicilina', 'látex', 'ninguna', 'AINEs', 'ninguna'])[1 + (f.paciente_id % 6)],
           'medicacion_actual', (ARRAY['ninguna', 'enalapril 10 mg', 'levotiroxina', 'ninguna', 'anticonceptivos'])[1 + (f.paciente_id % 5)],
           'enfermedades', (ARRAY['ninguna', 'hipertensión', 'diabetes tipo 2', 'ninguna', 'asma', 'hipotiroidismo'])[1 + (f.paciente_id % 6)],
           'embarazo', CASE WHEN f.paciente_id IN (7, 17) THEN 'no sabe' ELSE 'no' END,
           'fuma', f.paciente_id % 4 = 0,
           'anticoagulantes', f.paciente_id = 18,
           'sangrado_encias', f.paciente_id % 3 = 0,
           'ultima_visita', (ARRAY['menos de 1 año', '1 a 2 años', 'más de 2 años', 'nunca'])[1 + (f.paciente_id % 4)]),
       CASE WHEN f.paciente_id = 18 THEN 'Anticoagulado (acenocumarol): consultar con hematología antes de extracciones.'
            WHEN f.paciente_id IN (4, 5, 9, 13, 19, 23) THEN 'Paciente menor: anamnesis respondida por madre/padre/tutor.'
            ELSE 'Sin observaciones relevantes.' END,
       f.fecha_apertura, 4, 'lgimenez@turnos-odontologia.test', 'REAL'
FROM fichas_clinicas f;

INSERT INTO anamnesis (ficha_id, version, respuestas, texto_libre, fecha, autor_id, autor_nombre, autor_modo)
SELECT a.ficha_id, 2, a.respuestas || jsonb_build_object('medicacion_actual', 'metformina 850 mg', 'enfermedades', 'diabetes tipo 2'),
       'Actualización: diagnóstico reciente de diabetes tipo 2.', a.fecha + interval '30 days', 4,
       'lgimenez@turnos-odontologia.test', 'REAL'
FROM anamnesis a JOIN fichas_clinicas f ON f.id = a.ficha_id
WHERE a.version = 1 AND f.paciente_id IN (2, 8, 15);

-- Odontograma de adultos (piezas permanentes)
INSERT INTO odontograma_registros (ficha_id, pieza_fdi, cara, estado, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, v.pieza, v.cara, v.estado, 1, f.fecha_apertura + interval '1 hour', 4, 'lgimenez@turnos-odontologia.test', 'REAL'
FROM fichas_clinicas f
CROSS JOIN (VALUES
    (16, 'oclusal', 'caries'),
    (26, 'oclusal', 'obturado'),
    (36, 'mesial', 'caries'),
    (46, NULL, 'endodoncia'),
    (18, NULL, 'ausente'),
    (11, 'vestibular', 'sano'),
    (21, NULL, 'corona'),
    (37, 'distal', 'extraccion_indicada')) AS v(pieza, cara, estado)
WHERE f.paciente_id NOT IN (4, 5, 9, 13, 19, 23)
  AND (f.paciente_id + v.pieza) % 3 <> 0;

-- Odontograma de menores (piezas temporales, cuadrantes 5 a 8) y selladores en permanentes
INSERT INTO odontograma_registros (ficha_id, pieza_fdi, cara, estado, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, v.pieza, v.cara, v.estado, 1, f.fecha_apertura + interval '1 hour', 6, 'cruiz@turnos-odontologia.test', 'REAL'
FROM fichas_clinicas f
CROSS JOIN (VALUES
    (54, 'oclusal', 'caries'),
    (55, 'oclusal', 'obturado'),
    (64, NULL, 'sano'),
    (74, 'oclusal', 'caries'),
    (75, NULL, 'extraccion_indicada'),
    (84, NULL, 'sano'),
    (51, NULL, 'ausente'),
    (16, 'oclusal', 'sellador')) AS v(pieza, cara, estado)
WHERE f.paciente_id IN (4, 5, 9, 13, 19, 23);

-- Versión 2 sobre la misma pieza/cara: la caries de 16 oclusal pasa a obturada (sin sobrescribir)
INSERT INTO odontograma_registros (ficha_id, pieza_fdi, cara, estado, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT o.ficha_id, o.pieza_fdi, o.cara, 'obturado', 2, o.fecha + interval '20 days', 4, 'lgimenez@turnos-odontologia.test', 'REAL'
FROM odontograma_registros o
WHERE o.pieza_fdi = 16 AND o.cara = 'oclusal' AND o.estado = 'caries' AND o.version = 1;

-- Versión 2 en menores: 54 oclusal caries -> obturado
INSERT INTO odontograma_registros (ficha_id, pieza_fdi, cara, estado, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT o.ficha_id, o.pieza_fdi, o.cara, 'obturado', 2, o.fecha + interval '15 days', 6, 'cruiz@turnos-odontologia.test', 'REAL'
FROM odontograma_registros o
WHERE o.pieza_fdi = 54 AND o.cara = 'oclusal' AND o.version = 1;

-- Evoluciones: una por turno confirmado pasado de pacientes con ficha
INSERT INTO evoluciones (ficha_id, profesional_id, turno_id, version, texto, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, t.profesional_id, t.id, 1,
       tr.nombre || ': ' || (ARRAY[
           'Se realiza el procedimiento sin complicaciones. Indicaciones post-operatorias entregadas.',
           'Paciente refiere sensibilidad leve. Se indica control en 15 días.',
           'Procedimiento completo. Se refuerza técnica de cepillado e hilo dental.',
           'Sin hallazgos patológicos. Próximo control en 6 meses.'])[1 + (t.id % 4)::INTEGER],
       t.fin + interval '5 minutes',
       CASE t.profesional_id WHEN 1 THEN 4 WHEN 2 THEN 5 WHEN 3 THEN 6 ELSE 7 END,
       CASE t.profesional_id WHEN 1 THEN 'lgimenez@turnos-odontologia.test' WHEN 2 THEN 'msosa@turnos-odontologia.test'
            WHEN 3 THEN 'cruiz@turnos-odontologia.test' ELSE 'dfernandez@turnos-odontologia.test' END,
       'REAL'
FROM turnos t
JOIN fichas_clinicas f ON f.paciente_id = t.paciente_id
JOIN tratamientos tr ON tr.id = t.tipo_tratamiento_id
WHERE t.estado = 'confirmado' AND t.inicio < now() AND NOT t.es_sobreturno;

-- Corrección de evolución: versión 2 que reemplaza a la versión 1 (sin sobrescribir)
INSERT INTO evoluciones (ficha_id, profesional_id, turno_id, version, reemplaza_id, texto, fecha, autor_id, autor_nombre, autor_modo)
SELECT e.ficha_id, e.profesional_id, e.turno_id, 2, e.id,
       e.texto || ' [Corrección: se agrega anestesia local con articaína 4% 1:100.000, 1 carpule.]',
       e.fecha + interval '2 hours', e.autor_id, e.autor_nombre, e.autor_modo
FROM evoluciones e
WHERE e.id IN (SELECT min(id) FROM evoluciones GROUP BY ficha_id ORDER BY 1 LIMIT 3);

-- Firma con matrícula: todas las evoluciones vigentes salvo las 5 más recientes (quedan sin firmar
-- para probar el flujo de firma). El hash replica FirmaClinicaService.hashEvolucion.
INSERT INTO evolucion_firmas (evolucion_id, profesional_id, matricula, hash_contenido, firmada_at,
                              firmante_id, firmante_nombre, firmante_modo)
SELECT e.id, e.profesional_id, p.matricula,
       encode(sha256(convert_to(e.id || '|' || e.ficha_id || '|' || e.profesional_id || '|' || e.version || '|' || e.texto, 'UTF8')), 'hex'),
       e.fecha + interval '10 minutes', e.autor_id, e.autor_nombre, e.autor_modo
FROM evoluciones e JOIN profesionales p ON p.id = e.profesional_id
WHERE NOT EXISTS (SELECT 1 FROM evoluciones r WHERE r.reemplaza_id = e.id)
  AND e.id NOT IN (SELECT id FROM evoluciones ORDER BY fecha DESC LIMIT 5);

-- Consentimientos informados
INSERT INTO consentimientos (ficha_id, tipo, texto, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, 'tratamiento_general',
       'Declaro haber sido informado/a sobre el diagnóstico, el tratamiento propuesto, sus riesgos y alternativas, y presto mi consentimiento. (Texto de prueba)',
       1, f.fecha_apertura + interval '30 minutes', 2, 'recepcion@turnos-odontologia.test', 'REAL'
FROM fichas_clinicas f;

INSERT INTO consentimientos (ficha_id, tipo, texto, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, 'endodoncia',
       'Consentimiento específico para tratamiento de conducto: riesgos de fractura instrumental, dolor post-operatorio y necesidad de corona. (Texto de prueba)',
       1, f.fecha_apertura + interval '40 days', 7, 'dfernandez@turnos-odontologia.test', 'REAL'
FROM fichas_clinicas f WHERE f.paciente_id IN (2, 6, 8, 12, 18);

INSERT INTO consentimientos (ficha_id, tipo, texto, version, fecha, autor_id, autor_nombre, autor_modo)
SELECT f.id, 'tratamiento_general',
       'Versión 2 del consentimiento general: se agrega el uso de imágenes clínicas con fines de seguimiento. (Texto de prueba)',
       2, f.fecha_apertura + interval '60 days', 2, 'recepcion@turnos-odontologia.test', 'REAL'
FROM fichas_clinicas f WHERE f.paciente_id IN (1, 3);

-- Firmas electrónicas de consentimiento (proveedor simulado); los de endodoncia quedan sin firmar
INSERT INTO consentimiento_firmas (consentimiento_id, proveedor, referencia_proveedor, hash_documento, firmada_at,
                                   registrado_por_id, registrado_por_nombre, registrado_por_modo)
SELECT c.id, 'simulado', 'sim-prueba-' || c.id,
       encode(sha256(convert_to(c.id || '|' || c.ficha_id || '|' || c.tipo || '|' || c.version || '|' || c.texto, 'UTF8')), 'hex'),
       c.fecha + interval '5 minutes', 2, 'recepcion@turnos-odontologia.test', 'REAL'
FROM consentimientos c WHERE c.tipo = 'tratamiento_general';

-- historia_version = cantidad de cambios registrados en cada ficha
UPDATE fichas_clinicas f SET historia_version =
      (SELECT count(*) FROM anamnesis a WHERE a.ficha_id = f.id)
    + (SELECT count(*) FROM odontograma_registros o WHERE o.ficha_id = f.id)
    + (SELECT count(*) FROM evoluciones e WHERE e.ficha_id = f.id)
    + (SELECT count(*) FROM consentimientos c WHERE c.ficha_id = f.id),
    updated_at = now();

-- Copias de historia entregadas al titular (Ley 26.529 art. 14)
INSERT INTO historia_copias (paciente_id, solicitada_at, vence_at, entregada_at, hash_documento, token_jti) VALUES
    (2,  now() - interval '20 days', now() - interval '18 days', now() - interval '19 days',
        encode(sha256(convert_to('copia-prueba-paciente-2', 'UTF8')), 'hex'), 'jti-prueba-copia-0001'),
    (8,  now() - interval '6 days',  now() - interval '4 days',  now() - interval '5 days',
        encode(sha256(convert_to('copia-prueba-paciente-8', 'UTF8')), 'hex'), 'jti-prueba-copia-0002');

-- -------------------------------------------------------------------------------------
-- 11. Financiero: presupuestos, ítems, señas Mercado Pago y comprobantes ARCA
-- -------------------------------------------------------------------------------------
CREATE TEMP TABLE precios_prueba (tratamiento_id BIGINT PRIMARY KEY, precio NUMERIC(12,2));
INSERT INTO precios_prueba VALUES
    (1, 15000.00), (2, 28000.00), (3, 45000.00), (4, 120000.00), (5, 38000.00), (6, 18000.00),
    (7, 350000.00), (8, 95000.00), (9, 14000.00), (10, 12000.00), (11, 22000.00), (12, 25000.00);

INSERT INTO presupuestos (id, paciente_id, turno_id, estado, total, created_at, created_by) VALUES
    (1,  2,  NULL, 'emitido', 0, now() - interval '40 days', 'recepcion@turnos-odontologia.test'),
    (2,  6,  NULL, 'emitido', 0, now() - interval '35 days', 'recepcion@turnos-odontologia.test'),
    (3,  8,  NULL, 'emitido', 0, now() - interval '30 days', 'recepcion2@turnos-odontologia.test'),
    (4,  10, NULL, 'emitido', 0, now() - interval '25 days', 'recepcion@turnos-odontologia.test'),
    (5,  12, NULL, 'anulado', 0, now() - interval '22 days', 'recepcion@turnos-odontologia.test'),
    (6,  14, NULL, 'emitido', 0, now() - interval '18 days', 'recepcion2@turnos-odontologia.test'),
    (7,  4,  NULL, 'emitido', 0, now() - interval '15 days', 'recepcion@turnos-odontologia.test'),
    (8,  16, NULL, 'emitido', 0, now() - interval '12 days', 'recepcion@turnos-odontologia.test'),
    (9,  31, NULL, 'emitido', 0, now() - interval '9 days',  'recepcion2@turnos-odontologia.test'),
    (10, 1,  NULL, 'emitido', 0, now() - interval '5 days',  'recepcion@turnos-odontologia.test'),
    (11, 20, NULL, 'emitido', 0, now() - interval '3 days',  'recepcion@turnos-odontologia.test'),
    (12, 41, NULL, 'emitido', 0, now() - interval '1 day',   'recepcion@turnos-odontologia.test');

-- Asociar al próximo turno activo del paciente cuando existe (RN-FI-01: asociación opcional)
UPDATE presupuestos p SET turno_id = (SELECT t.id FROM turnos t WHERE t.paciente_id = p.paciente_id AND t.activo
                                      AND t.inicio > now() ORDER BY t.inicio LIMIT 1)
WHERE p.id IN (1, 2, 4, 6, 8, 10);

INSERT INTO items_presupuesto (presupuesto_id, tratamiento_id, cantidad, precio, descripcion, created_by)
SELECT v.pres, v.trat, v.cant, pp.precio, v.descr, 'recepcion@turnos-odontologia.test'
FROM (VALUES
    (1, 4, 1, 'Endodoncia pieza 46'), (1, 3, 1, 'Reconstrucción post-endodoncia'),
    (2, 3, 3, 'Obturaciones piezas 16, 26 y 36'), (2, 2, 1, 'Limpieza'),
    (3, 5, 1, 'Extracción pieza 37'), (3, 11, 1, 'Evaluación para implante'),
    (4, 8, 1, 'Blanqueamiento en consultorio'), (4, 2, 1, 'Limpieza previa'),
    (5, 7, 1, 'Ortodoncia: instalación (presupuesto anulado)'),
    (6, 7, 1, 'Ortodoncia: instalación de brackets'), (6, 6, 12, 'Controles mensuales x 12'),
    (7, 9, 2, 'Controles odontopediátricos'), (7, 10, 4, 'Selladores en molares permanentes'),
    (8, 3, 2, 'Obturaciones'), (8, 1, 1, 'Consulta inicial'),
    (9, 2, 2, 'Limpiezas semestrales'), (9, 3, 1, 'Obturación'),
    (10, 2, 1, 'Limpieza y profilaxis'), (10, 3, 1, 'Obturación pieza 36'),
    (11, 4, 1, 'Endodoncia pieza 21'), (11, 12, 1, 'Atención de urgencia'),
    (12, 1, 1, 'Consulta y diagnóstico')) AS v(pres, trat, cant, descr)
JOIN precios_prueba pp ON pp.tratamiento_id = v.trat;

UPDATE presupuestos p SET total = (SELECT COALESCE(sum(i.cantidad * i.precio), 0)
                                   FROM items_presupuesto i WHERE i.presupuesto_id = p.id AND i.activo),
                          updated_at = CASE WHEN p.estado = 'anulado' THEN now() - interval '20 days' END,
                          updated_by = CASE WHEN p.estado = 'anulado' THEN 'recepcion@turnos-odontologia.test' END;

-- Señas en todos los estados (aprobada conciliada, aprobada sin conciliar por referencia
-- inexistente, pendiente, rechazada, cancelada y reembolsada)
INSERT INTO senias (id, turno_id, presupuesto_id, monto, estado, payment_id, external_reference, idempotency_key,
                    fecha_aprobacion, conciliado, created_at, created_by, updated_at, updated_by)
SELECT v.id,
       (SELECT turno_id FROM presupuestos WHERE id = v.pres),
       v.pres, v.monto, v.estado, v.payment_id, 'senia-prueba-' || lpad(v.id::TEXT, 4, '0'),
       'idem-senia-prueba-' || lpad(v.id::TEXT, 4, '0'),
       CASE WHEN v.estado IN ('aprobado', 'reembolsado') THEN now() - (v.dias || ' days')::interval END,
       v.conc, now() - (v.dias || ' days')::interval - interval '1 hour', 'recepcion@turnos-odontologia.test',
       CASE WHEN v.estado <> 'pendiente' THEN now() - (v.dias || ' days')::interval END,
       CASE WHEN v.estado <> 'pendiente' THEN 'webhook-mercadopago' END
FROM (VALUES
    (1, 1,  50000.00, 'aprobado',    'PRUEBA-MP-1000000001', TRUE,  38),
    (2, 2,  30000.00, 'aprobado',    'PRUEBA-MP-1000000002', TRUE,  33),
    (3, 3,  20000.00, 'rechazado',   'PRUEBA-MP-1000000003', FALSE, 28),
    (4, 4,  40000.00, 'pendiente',   NULL,                   FALSE, 2),
    (5, 6, 100000.00, 'aprobado',    'PRUEBA-MP-1000000005', TRUE,  16),
    (6, 7,  10000.00, 'cancelado',   NULL,                   FALSE, 14),
    (7, 8,  25000.00, 'reembolsado', 'PRUEBA-MP-1000000007', TRUE,  10),
    (8, 9,  35000.00, 'aprobado',    'PRUEBA-MP-1000000008', TRUE,  8),
    (9, 10, 20000.00, 'pendiente',   NULL,                   FALSE, 1),
    (10, 11, 60000.00, 'aprobado',   'PRUEBA-MP-1000000010', TRUE,  2)) AS v(id, pres, monto, estado, payment_id, conc, dias);

-- Seña aprobada que referencia un turno inexistente: queda sin conciliar (MP-3)
INSERT INTO senias (id, turno_id, presupuesto_id, monto, estado, payment_id, external_reference, idempotency_key,
                    fecha_aprobacion, conciliado, created_by, updated_at, updated_by)
VALUES (11, 999999, NULL, 15000.00, 'aprobado', 'PRUEBA-MP-1000000011', 'senia-prueba-0011', 'idem-senia-prueba-0011',
        now() - interval '4 days', FALSE, 'recepcion@turnos-odontologia.test', now() - interval '4 days', 'webhook-mercadopago');

-- Comprobantes ARCA: factura emitida, pendiente, rechazada y nota de crédito por reembolso
INSERT INTO comprobantes (senia_id, presupuesto_id, turno_id, tipo, estado, monto, concepto, cae, fecha_emision,
                          intentos, ultimo_error, conciliado, created_by, created_at, updated_at, updated_by)
SELECT s.id, s.presupuesto_id, s.turno_id, v.tipo, v.estado, s.monto,
       'Seña de tratamiento odontológico - presupuesto ' || COALESCE(s.presupuesto_id::TEXT, 's/n'),
       CASE WHEN v.estado = 'emitido' THEN '7' || lpad((90000000000000 + s.id * 10 + (v.tipo = 'nota_credito')::INT)::TEXT, 13, '0') END,
       CASE WHEN v.estado = 'emitido' THEN s.fecha_aprobacion + interval '1 hour' END,
       v.intentos, v.error, v.estado = 'emitido', 'admin@turnos-odontologia.test',
       s.fecha_aprobacion + interval '30 minutes', s.fecha_aprobacion + interval '1 hour', 'admin@turnos-odontologia.test'
FROM senias s
JOIN (VALUES
    (1,  'factura',      'emitido',   1, NULL),
    (2,  'factura',      'emitido',   1, NULL),
    (5,  'factura',      'rechazado', 2, 'ARCA rechazó el comprobante: 10015 - CUIT receptor inválido (error de prueba)'),
    (7,  'factura',      'emitido',   1, NULL),
    (7,  'nota_credito', 'emitido',   1, NULL),
    (8,  'factura',      'pendiente', 0, NULL)) AS v(senia, tipo, estado, intentos, error) ON v.senia = s.id;
-- La seña 10 (aprobada) queda sin comprobante: para probar la emisión manual.
DROP TABLE precios_prueba;

-- -------------------------------------------------------------------------------------
-- 12. Lista de espera (activa, ofrecida, adjudicada, expirada; con y sin depósito)
-- -------------------------------------------------------------------------------------
INSERT INTO lista_espera (candidato_id, profesional_id, sillon_id, inicio_deseado, fin_deseado, deposito_requerido,
                          senia_id, estado, ofrecida_en, turno_id, created_by, created_at)
VALUES
    (21, 1, 1, pg_temp.ts(pg_temp.dia_habil(2), '09:00'), pg_temp.ts(pg_temp.dia_habil(2), '13:00'), FALSE, NULL, 'activa', NULL, NULL, 'recepcion@turnos-odontologia.test', now() - interval '3 days'),
    (22, 1, 1, pg_temp.ts(pg_temp.dia_habil(2), '09:00'), pg_temp.ts(pg_temp.dia_habil(3), '18:00'), TRUE,  NULL, 'activa', NULL, NULL, 'recepcion@turnos-odontologia.test', now() - interval '2 days'),
    (25, 2, 2, pg_temp.ts(pg_temp.dia_habil(4), '10:00'), pg_temp.ts(pg_temp.dia_habil(4), '19:00'), TRUE,  NULL, 'activa', NULL, NULL, 'recepcion2@turnos-odontologia.test', now() - interval '1 day'),
    (26, 4, 3, pg_temp.ts(pg_temp.dia_habil(5), '14:00'), pg_temp.ts(pg_temp.dia_habil(5), '20:00'), FALSE, NULL, 'activa', NULL, NULL, 'recepcion@turnos-odontologia.test', now() - interval '5 hours'),
    (9,  3, 3, pg_temp.ts(pg_temp.dia_habil(2), '08:00'), pg_temp.ts(pg_temp.dia_habil(2), '14:00'), FALSE, NULL, 'activa', NULL, NULL, 'recepcion@turnos-odontologia.test', now() - interval '4 days'),
    (27, 1, 1, pg_temp.ts(pg_temp.dia_habil(1), '09:00'), pg_temp.ts(pg_temp.dia_habil(1), '13:00'), TRUE,  4,    'ofrecida', now() - interval '5 minutes', NULL, 'recepcion@turnos-odontologia.test', now() - interval '6 days'),
    (28, 4, 3, pg_temp.ts(pg_temp.dia_habil(-4), '14:00'), pg_temp.ts(pg_temp.dia_habil(-4), '20:00'), FALSE, NULL, 'expirada', now() - interval '8 days', NULL, 'recepcion@turnos-odontologia.test', now() - interval '12 days');

-- Adjudicada: la entrada quedó vinculada a un turno reasignado desde la waitlist con seña aprobada
UPDATE turnos SET origen = 'waitlist', estado = 'confirmado', updated_at = now() - interval '9 days',
                  updated_by = 'motor-waitlist'
WHERE id = (SELECT min(id) FROM turnos WHERE activo AND paciente_id = 29 AND inicio > now() - interval '30 days');

INSERT INTO lista_espera (candidato_id, profesional_id, sillon_id, inicio_deseado, fin_deseado, deposito_requerido,
                          senia_id, estado, ofrecida_en, turno_id, created_by, created_at)
SELECT 29, t.profesional_id, t.sillon_id, t.inicio - interval '2 hours', t.fin + interval '2 hours', TRUE,
       8, 'adjudicada', now() - interval '9 days', t.id, 'recepcion@turnos-odontologia.test', now() - interval '15 days'
FROM turnos t WHERE t.origen = 'waitlist' AND t.paciente_id = 29 LIMIT 1;

-- -------------------------------------------------------------------------------------
-- 13. Métricas de ocupación y ausentismo de meses cerrados (con metodología obligatoria)
-- -------------------------------------------------------------------------------------
INSERT INTO metricas_ocupacion (sillon_id, periodo, ocupacion, ausentismo, metodologia, calculada_en, created_by)
SELECT s.id, to_char(date_trunc('month', current_date) - make_interval(months => m), 'YYYY-MM'),
       round((55 + (s.id * 7 + m * 5) % 30)::NUMERIC + 0.1234, 4),
       round((4 + (s.id * 3 + m * 2) % 9)::NUMERIC + 0.5, 4),
       'ocupacion=100*minutos(turnos activos+bloqueos activos)/minutos(horarios activos menos feriados); '
       || 'ausentismo=100*turnos inasistentes/turnos confirmados (cancelados excluidos); '
       || 'fuente=turnos,bloqueos,horarios,feriados; zona=America/Argentina/Buenos_Aires; '
       || 'periodo=YYYY-MM; meta=(TBD) -> forma de obtencion (piloto y PO Parte 3)',
       date_trunc('month', current_date) - make_interval(months => m - 1) + interval '2 hours',
       'datos-de-prueba'
FROM sillones s CROSS JOIN generate_series(1, 3) AS m
WHERE s.activo;

-- -------------------------------------------------------------------------------------
-- 14. Cumplimiento: solo solicitudes pendientes y una falla (la evidencia COMPLETADA se
--     genera ejecutando exportación/respaldo/restore reales, no se inventa)
-- -------------------------------------------------------------------------------------
INSERT INTO cumplimiento_operaciones (tipo, alcance, paciente_id, estado, formato, motivo, solicitado_por,
                                      solicitado_at, plazo_vence_at)
VALUES ('EXPORTACION', 'PACIENTE', 15, 'SOLICITADA', 'JSON-v1', 'Pedido del titular (art. 14 Ley 25.326) - prueba',
        'admin@turnos-odontologia.test', now() - interval '2 hours', now() + interval '46 hours');

INSERT INTO cumplimiento_operaciones (tipo, alcance, estado, formato, motivo, solicitado_por, solicitado_at, error)
VALUES ('RESPALDO', 'COMPLETA', 'FALLIDA', 'JSON-v1', 'Respaldo programado - prueba de alerta de fallo',
        'system', now() - interval '1 day', 'destino de respaldo no configurado (BACKUP_STORAGE) - prueba');

-- Subprocesadores sin cláusulas AAIP 198/2023 publicadas: bloquean el ítem del dossier
INSERT INTO subprocesadores (nombre, servicio, ubicacion_datos, clausulas_aaip_referencia, dpa_referencia, created_by) VALUES
    ('Proveedor WhatsApp Business API (a definir)', 'Envío de confirmaciones y recordatorios', NULL, NULL, NULL, 'datos-de-prueba'),
    ('Proveedor de pagos Mercado Pago', 'Cobro de señas', 'Argentina (a confirmar)', NULL, NULL, 'datos-de-prueba'),
    ('Hosting de prueba (a definir)', 'Infraestructura y respaldos', NULL, NULL, NULL, 'datos-de-prueba');

-- -------------------------------------------------------------------------------------
-- 15. Sincronizar secuencias con los ids explícitos
-- -------------------------------------------------------------------------------------
SELECT setval('sillones_id_seq',        (SELECT max(id) FROM sillones));
SELECT setval('profesionales_id_seq',   (SELECT max(id) FROM profesionales));
SELECT setval('tratamientos_id_seq',    (SELECT max(id) FROM tratamientos));
SELECT setval('horarios_id_seq',        (SELECT max(id) FROM horarios));
SELECT setval('usuarios_id_seq',        (SELECT max(id) FROM usuarios));
SELECT setval('pacientes_id_seq',       (SELECT max(id) FROM pacientes));
SELECT setval('bloqueos_id_seq',        (SELECT max(id) FROM bloqueos));
SELECT setval('plantillas_whatsapp_id_seq', (SELECT max(id) FROM plantillas_whatsapp));
SELECT setval('fichas_clinicas_id_seq', (SELECT max(id) FROM fichas_clinicas));
SELECT setval('presupuestos_id_seq',    (SELECT max(id) FROM presupuestos));
SELECT setval('senias_id_seq',          (SELECT max(id) FROM senias));
