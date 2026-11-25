# Spec Delta

## ADDED Requirements

### Requirement: Evento de consulta de métricas en el catálogo
El catálogo de auditoría SHALL admitir `metricas.consultadas` (entidad `metricas`, sin `entidad_id`) y el evento MUST persistirse en el log append-only con su cadena de integridad.

#### Scenario: Consulta persistida
- **GIVEN** el publicador real de auditoría
- **WHEN** se consulta el tablero de métricas
- **THEN** existe una fila con `accion=metricas.consultadas`, `entidad=metricas` y el actor de la consulta
