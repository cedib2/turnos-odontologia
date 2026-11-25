# Design

## Decisiones

| # | Decisión | Motivo |
|---|---|---|
| D1 | Configuración por propiedades `precio.plan.*`, sin tabla | Es un único plan para un consultorio único (A2/B3); un CRUD sería sobrediseño para governance BAJO. Las propiedades no son secretos, por eso no van a `.env.example`. |
| D2 | `PENDIENTE_DEFINICION` sin cifra cuando falta el monto | RN-GL-03 y DD-06: no se publica una cifra que el PO no decidió. |
| D3 | Invariantes en el record `PrecioPublico` | El total publicado coincide con el monto, no hay packs ni desglose de API: el modelo no puede construirse violando A5. |
| D4 | Fail-fast ante configuración parcial o inválida | Evita publicar un precio a medias (monto sin moneda, monto ≤ 0, moneda no ISO 4217). |
| D5 | `GET /precio` sin `@Profile` | Es información pública por definición (US-016); no lee identidad ni credenciales. |
| D6 | Comisiones de Mercado Pago como `(TBD)` explícito | No es un costo de API del producto sino del procesador de pago sobre cada cobro; afirmar que está incluido o excluido sería decidir por convención. |

## Contrato de respuesta

```json
{
  "estado": "PUBLICADO | PENDIENTE_DEFINICION",
  "modelo": "PLANO_TODO_INCLUIDO",
  "monto": 25000, "moneda": "ARS", "periodicidad": "mensual", "total": 25000,
  "incluye": [{"codigo": "whatsapp", "descripcion": "...", "cargoAdicional": 0}],
  "cargosAdicionales": [],
  "desgloseCostosApi": false,
  "pendientes": ["(TBD) ..."],
  "decision": "C-01 A5 — ..."
}
```
