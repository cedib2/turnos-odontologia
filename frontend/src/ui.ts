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
