# Sistema de diseño

Política visual elegida: **`system-only`** (sistema de diseño sin mockups previos). Todo vive en `public/css/sistema.css`; las páginas solo usan sus clases y tokens.

## Principios

- **Clínico y sereno:** un solo acento (verde azulado), neutros cálidos y mucho aire. La información sensible no compite con decoración.
- **Mobile-first:** el paciente reserva desde el celular (link o QR). Los objetivos táctiles miden al menos 44 px (`--alto-tactil`).
- **Accesible:** foco visible, contraste AA, `aria-live` para mensajes, tablas que se apilan en pantallas chicas, respeto a `prefers-reduced-motion`.
- **Sin dependencias:** fuentes del sistema y CSS propio, sin frameworks.

## Tokens

| Grupo | Tokens |
|---|---|
| Color | `--c-fondo`, `--c-superficie`, `--c-superficie-2`, `--c-borde`, `--c-texto`, `--c-texto-suave`, `--c-texto-tenue`, `--c-acento`, `--c-acento-suave`; semánticos `--c-ok`, `--c-alerta`, `--c-error`, `--c-info` y sus variantes `-suave` |
| Tipografía | `--f-texto`, `--f-mono`; escala `--t-xs` a `--t-2xl` (las grandes son fluidas con `clamp`) |
| Espaciado | `--e-1` (4 px) a `--e-7` (48 px) |
| Forma | `--r-sm`, `--r-md`, `--r-lg`, `--r-pill`; `--sombra-1`, `--sombra-2`; `--foco` |

**Tema oscuro:** se aplica con `prefers-color-scheme: dark`, salvo que `<html data-tema="claro">` lo fuerce; `data-tema="oscuro"` lo fuerza en cualquier sistema.

## Componentes

| Clase | Uso |
|---|---|
| `.contenedor`, `.pila`, `.fila`, `.rejilla`, `.rejilla--2` | Layout |
| `.encabezado`, `.nav`, `.nav__enlace[aria-current]`, `.nav__boton` | Navegación; en menos de 768 px se colapsa en un menú |
| `.tarjeta`, `.tarjeta__titulo` | Contenedor principal de contenido |
| `.btn`, `--secundario`, `--fantasma`, `--peligro`, `--chico`, `--bloque` | Acciones; `aria-busy="true"` muestra el estado de carga |
| `.formulario`, `.formulario__fila`, `.campo`, `.control`, `.campo__ayuda` | Formularios |
| `.aviso--info/ok/alerta/error` | Mensajes en línea |
| `.insignia--ok/alerta/error/info/marca` | Estados (turno, ítem del dossier, métrica) |
| `.pestanas` | Pestañas con `role="tab"` y `aria-selected` |
| `.tabla` | Tablas; en pantallas chicas cada fila se apila usando `data-etiqueta` |
| `.metrica__valor`, `.barra` | Métricas y progreso |
| `.huecos`, `.hueco[aria-pressed]` | Horarios disponibles en la reserva |
| `.pasos` | Progreso del flujo de reserva |
| `.evento`, `.semana` | Agenda por día y por semana |
| `.toast`, `dialog.dialogo` | Notificaciones y confirmaciones |

## Puntos de quiebre

- Menos de 30 rem (480 px): pasos compactos y eventos de agenda en una columna.
- Menos de 48 rem (768 px): menú colapsado y tablas apiladas.
- Menos de 64 rem (1024 px): la semana pasa de 7 columnas a una lista.
