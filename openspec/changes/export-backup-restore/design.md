# Design — C-17

## Volcado genérico por catálogo
`VolcadoBaseDatos` lee `pg_tables` del esquema actual (excepto `flyway_schema_history`) y guarda cada fila como el texto de `to_jsonb(t)`, ordenada por clave primaria. Ventaja: las tablas que agreguen las Partes 2 y 3 (C-10..C-16) entran en el respaldo sin tocar este código, cumpliendo el acuerdo de trabajar por contratos.

## Restore y verificación
1. Se verifica que la base aislada **no** sea la productiva (`current_database()@host:puerto`). Recién después se ejecuta `flyway clean` sobre ella.
2. Se verifica el checksum SHA-256 del artefacto descifrado contra el registrado (detecta alteración en el destino; AES-GCM también la detecta).
3. `flyway migrate`, borrado de semillas, inserción en orden topológico de FKs con `jsonb_populate_record` (omitiendo columnas generadas), `setval` de secuencias.
4. Comparación fila a fila (`to_jsonb` en UTC) entre lo restaurado y el respaldo. Todo en una transacción.

## Alternativas descartadas
- `pg_dump`/`pg_restore`: agrega un binario externo al runtime (restricción de no instalar servicios nuevos) y no permite verificar fila a fila desde la app.
- Restore en otro esquema de la misma base: no es un entorno aislado y comparte permisos con la base productiva.

## Límites declarados
- El guardián de aislamiento compara base/host/puerto; dos alias de red al mismo servidor y base no se detectan. Se documenta como requisito operativo: la base aislada debe ser otra base.
- La copia de historia clínica depende de C-13; mientras no exista, la exportación lo declara `(pendiente: C-13 no implementado)`.
