# Design

## Decisiones

| # | Decisión | Motivo |
|---|---|---|
| D1 | Canal link/QR = `turnos.token_publico IS NOT NULL` | Es la fuente aprobada en C-01 §Métricas. `origen` es texto libre hasta C-09 y no tiene valores canónicos; usarlo sería inventar una convención. Si C-09 fija un valor canónico de `origen`, la fórmula cambia con aprobación del PO. |
| D2 | Se cuentan todos los turnos creados (también los luego desactivados) por `created_at` | La métrica mide el canal de creación, no el estado final. |
| D3 | Período semiabierto `[desde, hasta)` obligatorio | Evita doble conteo entre períodos contiguos; DD-06 exige período explícito. |
| D4 | Puerto `FuenteMetrica` + `NO_DISPONIBLE` con dependencia | Parte 4 avanza sin esperar a Partes 2/3 y sin cifras decorativas; cada parte enchufa su bean. |
| D5 | Denominador 0 → `SIN_DATOS` sin porcentaje | No se publica 0 % ni 100 % sin casos. |
| D6 | Porcentaje con 2 decimales, `HALF_UP` | Reproducible y auditable. |
| D7 | Superficie en `@Profile({"dev","test"})` y actor vía `CurrentActorProvider` | Las guardias de C-04 prohíben leer identidad fuera de `auth/`; se sigue el mismo criterio que la agenda hasta que exista el adaptador de staff. |

## Contrato del puerto

```java
public interface FuenteMetrica {
    String codigo();                       // uno de CatalogoMetricas.*
    ResultadoMetrica calcular(Periodo periodo, MetodologiaMetrica metodologia);
}
```
