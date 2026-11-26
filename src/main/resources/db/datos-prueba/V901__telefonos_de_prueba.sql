-- =====================================================================================
-- Datos de prueba (solo dev): teléfonos ficticios de Argentina para TODOS los pacientes,
-- en formato internacional sin signos (apto para wa.me): 549 + 261 (Mendoza) + 555 + 4 dígitos.
--   paciente 1 -> 5492615550101, paciente 2 -> 5492615550102, ...
-- Son números inventados (prefijo 555), no pertenecen a personas reales.
-- Va en una migración nueva para no alterar el checksum de V900 ya aplicado.
-- =====================================================================================

UPDATE pacientes
SET telefono = '549261555' || lpad((100 + id)::TEXT, 4, '0'),
    updated_at = now(),
    updated_by = 'datos-de-prueba'
WHERE created_by = 'datos-de-prueba' OR telefono IS NULL OR btrim(telefono) = '';

-- Las notificaciones ya cargadas quedan coherentes con el teléfono actual del paciente del turno.
UPDATE notificaciones n
SET destinatario = p.telefono,
    updated_at = now()
FROM turnos t
JOIN pacientes p ON p.id = t.paciente_id
WHERE t.id = n.turno_id
  AND n.destinatario IS DISTINCT FROM p.telefono;
