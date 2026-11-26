// Utilidades de interfaz sin dependencias: creación de nodos, mensajes y estados de carga.

type Hijo = Node | string | number | null | undefined | false;
type Atributos = Record<string, string | number | boolean | null | undefined | EventListener>;

/** Crea un elemento. Los textos se insertan como nodos de texto (nunca como HTML). */
export function el<K extends keyof HTMLElementTagNameMap>(
  etiqueta: K,
  atributos: Atributos = {},
  ...hijos: Hijo[]
): HTMLElementTagNameMap[K] {
  const nodo = document.createElement(etiqueta);
  for (const [clave, valor] of Object.entries(atributos)) {
    if (valor === null || valor === undefined || valor === false) continue;
    if (clave.startsWith('on') && typeof valor === 'function') {
      nodo.addEventListener(clave.slice(2).toLowerCase(), valor);
    } else if (valor === true) {
      nodo.setAttribute(clave, '');
    } else {
      nodo.setAttribute(clave, String(valor));
    }
  }
  for (const hijo of hijos) {
    if (hijo === null || hijo === undefined || hijo === false) continue;
    nodo.append(hijo instanceof Node ? hijo : String(hijo));
  }
  return nodo;
}

export function $(selector: string, raiz: ParentNode = document): HTMLElement {
  const nodo = raiz.querySelector<HTMLElement>(selector);
  if (!nodo) throw new Error(`No se encontró ${selector}`);
  return nodo;
}

export function vaciar(nodo: Element, ...hijos: Hijo[]): void {
  nodo.replaceChildren();
  for (const hijo of hijos) {
    if (hijo === null || hijo === undefined || hijo === false) continue;
    nodo.append(hijo instanceof Node ? hijo : String(hijo));
  }
}

export type TipoAviso = 'info' | 'ok' | 'alerta' | 'error';

export function aviso(tipo: TipoAviso, ...contenido: Hijo[]): HTMLElement {
  return el('div', { class: `aviso aviso--${tipo}`, role: tipo === 'error' ? 'alert' : 'status' }, el('div', {}, ...contenido));
}

export function insignia(texto: string, tipo: 'ok' | 'alerta' | 'error' | 'info' | 'marca' | '' = ''): HTMLElement {
  return el('span', { class: tipo ? `insignia insignia--${tipo}` : 'insignia' }, texto);
}

export function toast(mensaje: string, tipo: 'normal' | 'error' = 'normal'): void {
  let contenedor = document.querySelector<HTMLElement>('.toasts');
  if (!contenedor) {
    contenedor = el('div', { class: 'toasts', 'aria-live': 'polite' });
    document.body.append(contenedor);
  }
  const nodo = el('div', { class: tipo === 'error' ? 'toast toast--error' : 'toast', role: 'status' }, mensaje);
  contenedor.append(nodo);
  window.setTimeout(() => nodo.remove(), 5000);
}

/** Ejecuta una acción con el botón en estado de carga y evita el doble envío. */
export async function conCarga<T>(boton: HTMLButtonElement, accion: () => Promise<T>): Promise<T | undefined> {
  if (boton.getAttribute('aria-busy') === 'true') return undefined;
  boton.setAttribute('aria-busy', 'true');
  boton.disabled = true;
  try {
    return await accion();
  } finally {
    boton.removeAttribute('aria-busy');
    boton.disabled = false;
  }
}

export function campo(id: string, etiqueta: string, control: HTMLElement, ayuda?: string): HTMLElement {
  control.id = id;
  if (ayuda) control.setAttribute('aria-describedby', `${id}-ayuda`);
  return el('div', { class: 'campo' },
    el('label', { for: id }, etiqueta),
    control,
    ayuda ? el('span', { class: 'campo__ayuda', id: `${id}-ayuda` }, ayuda) : null);
}

export function opciones(select: HTMLSelectElement, items: { valor: string; texto: string }[], vacio?: string): void {
  select.replaceChildren();
  if (vacio !== undefined) select.append(el('option', { value: '' }, vacio));
  for (const item of items) select.append(el('option', { value: item.valor }, item.texto));
}

/** Confirmación accesible con <dialog>. */
export function confirmar(titulo: string, texto: string, accion = 'Confirmar', peligro = false): Promise<boolean> {
  return new Promise((resolver) => {
    const dialogo = el('dialog', { class: 'dialogo', 'aria-labelledby': 'dialogo-titulo' });
    const cerrar = (valor: boolean) => { dialogo.close(); dialogo.remove(); resolver(valor); };
    dialogo.append(
      el('div', { class: 'dialogo__cuerpo' }, el('h2', { id: 'dialogo-titulo' }, titulo), el('p', {}, texto)),
      el('div', { class: 'dialogo__acciones' },
        el('button', { class: 'btn btn--secundario', type: 'button', onclick: () => cerrar(false) }, 'Volver'),
        el('button', { class: peligro ? 'btn btn--peligro' : 'btn', type: 'button', onclick: () => cerrar(true) }, accion)),
    );
    dialogo.addEventListener('cancel', () => cerrar(false));
    document.body.append(dialogo);
    dialogo.showModal();
  });
}

export function porcentaje(valor: number | null): string {
  return valor === null ? '—' : `${valor.toLocaleString('es-AR', { maximumFractionDigits: 2 })} %`;
}

/** «Sillón 3» a partir del número, sin duplicar el prefijo si el número ya lo trae. */
export function etiquetaSillon(numero: string): string {
  return /^sill[oó]n\b/i.test(numero.trim()) ? numero.trim() : `Sillón ${numero}`;
}

export interface DatoTurno {
  etiqueta: string;
  valor: string;
}

export interface AccionConfirmacion {
  texto: string;
  primaria?: boolean;
  /** Se ejecuta después de cerrar el diálogo. */
  alHacer?: () => void;
}

export interface OpcionesConfirmacionTurno {
  /** Tipo de turno o motivo (p. ej. «Consulta y diagnóstico»). */
  motivo: string;
  /** Estado real del turno: solo `confirmado` muestra «✓ Confirmado». */
  estado: string;
  datos: DatoTurno[];
  /** Texto del banner informativo. */
  mensaje: Hijo;
  acciones: AccionConfirmacion[];
}

function insigniaEstado(estado: string): HTMLElement {
  const valor = estado.toLowerCase();
  if (valor === 'confirmado') return insignia('✓ Confirmado', 'ok');
  if (valor === 'reservado') return insignia('Reservado', 'marca');
  return insignia(valor.charAt(0).toUpperCase() + valor.slice(1), 'info');
}

/** Ventana de confirmación de un turno recién creado: resumen, aviso y acciones. */
export function confirmacionTurno(opciones: OpcionesConfirmacionTurno): void {
  const dialogo = el('dialog', {
    class: 'dialogo dialogo--confirmacion', 'aria-labelledby': 'confirmacion-titulo',
    'aria-describedby': 'confirmacion-mensaje',
  });
  const cerrar = (despues?: () => void) => { dialogo.close(); dialogo.remove(); despues?.(); };
  const botones = opciones.acciones.map((accion) => el('button', {
    class: accion.primaria ? 'btn' : 'btn btn--secundario', type: 'button',
    onclick: () => cerrar(accion.alHacer),
  }, accion.texto));

  dialogo.append(
    el('div', { class: 'dialogo__cuerpo confirmacion' },
      el('div', { class: 'confirmacion__icono', 'aria-hidden': 'true' }, '✓'),
      el('h2', { id: 'confirmacion-titulo', class: 'confirmacion__titulo' }, '¡Turno creado!'),
      el('section', { class: 'confirmacion__tarjeta', 'aria-label': 'Datos del turno' },
        el('div', { class: 'confirmacion__cabecera' },
          el('strong', { class: 'confirmacion__motivo' }, opciones.motivo),
          insigniaEstado(opciones.estado)),
        el('dl', { class: 'confirmacion__datos' },
          ...opciones.datos.flatMap((dato) => [el('dt', {}, dato.etiqueta), el('dd', {}, dato.valor)]))),
      el('div', { class: 'confirmacion__banner', id: 'confirmacion-mensaje', role: 'note' },
        el('span', { class: 'confirmacion__banner-icono', 'aria-hidden': 'true' }, 'i'),
        el('div', {}, opciones.mensaje))),
    el('div', { class: 'dialogo__acciones confirmacion__acciones' }, ...botones));
  // Escape equivale a la última acción (cerrar / volver).
  const ultima = opciones.acciones[opciones.acciones.length - 1];
  dialogo.addEventListener('cancel', (evento) => { evento.preventDefault(); cerrar(ultima?.alHacer); });
  document.body.append(dialogo);
  dialogo.showModal();
  botones.find((_, i) => opciones.acciones[i]?.primaria)?.focus();
}
