// Historia clínica del titular (C-13, Ley 26.529 art. 14): el paciente abre el link que le envió el
// consultorio, confirma su identidad y ve SOLO su propia historia, en modo lectura. El token del link está
// atado a su paciente: no hay forma de pedir la ficha de otra persona.

import { api, ApiError } from '../api.js';
import { fechaHora } from '../fechas.js';
import { montarLayout } from '../layout.js';
import type { HistoriaClinica, HistoriaDelTitular } from '../tipos.js';
import { $, aviso, campo, conCarga, el, insignia, vaciar } from '../ui.js';

montarLayout('paciente');
const main = $('#contenido');
const token = new URLSearchParams(location.search).get('token');
const ruta = (accion: string) => `/publico/v1/historia/${accion}?token=${encodeURIComponent(token ?? '')}`;

const MOTIVOS: Record<string, string> = {
  identidad_no_verificada: 'No pudimos confirmar tu identidad. Revisá el número de documento.',
  token_vencido: 'Este link venció. Pedile uno nuevo al consultorio.',
  token_no_valido: 'El link no es válido. Revisá que esté completo o pedí uno nuevo.',
  jti_reutilizado: 'Este link ya no está disponible. Pedile uno nuevo al consultorio.',
  scope_no_permitido: 'Este link no sirve para ver la historia clínica.',
};

const contenido = el('div', { class: 'pila' });
vaciar(main, el('div', { style: 'max-width: 48rem; margin-inline: auto' },
  el('div', { class: 'titulo-pagina' },
    el('h1', {}, 'Mi historia clínica'),
    el('p', {}, 'Tu información odontológica, tal como la registró el consultorio. Es de solo lectura.')),
  contenido));

function motivo(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return error.message;
    return MOTIVOS[error.codigo ?? ''] ?? 'No pudimos mostrar tu historia. Intentá de nuevo más tarde.';
  }
  return 'Ocurrió un error inesperado.';
}

if (!token) {
  vaciar(contenido, aviso('error', el('p', {}, el('strong', {}, 'El link está incompleto.')),
    el('p', {}, 'Abrí el link completo que te envió el consultorio.')));
} else {
  pasoIdentidad();
}

function pasoIdentidad(): void {
  const documento = el('input', {
    class: 'control', type: 'text', inputmode: 'numeric', autocomplete: 'off', required: true, maxlength: 32,
  });
  const mensajes = el('div', { 'aria-live': 'polite' });
  const ver = el('button', { class: 'btn btn--bloque', type: 'submit' }, 'Ver mi historia');
  const formulario = el('form', { class: 'formulario tarjeta', novalidate: true },
    el('h2', { style: 'margin: 0; font-size: var(--t-lg)' }, 'Confirmá que sos vos'),
    campo('documento', 'Número de documento', documento, 'El que registraste en el consultorio, sin puntos.'),
    mensajes, ver);
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    vaciar(mensajes);
    if (!documento.value.trim()) {
      mensajes.append(aviso('error', 'Ingresá tu número de documento.'));
      return;
    }
    void conCarga(ver, async () => {
      try {
        const respuesta = await api.post<HistoriaDelTitular>(ruta('consulta'), { documento: documento.value.trim() });
        pintar(respuesta, documento.value.trim());
      } catch (error) {
        mensajes.append(aviso('error', motivo(error)));
      }
    });
  });
  vaciar(contenido, formulario);
  documento.focus();
}

function pintar(respuesta: HistoriaDelTitular, documento: string): void {
  const descargar = el('button', { class: 'btn btn--secundario', type: 'button' }, 'Descargar copia');
  const estado = el('div', { 'aria-live': 'polite' });
  descargar.addEventListener('click', () => conCarga(descargar, async () => {
    vaciar(estado);
    try {
      const archivo = await fetch(ruta('copia'), {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ documento }),
      });
      if (!archivo.ok) throw new ApiError(archivo.status, await archivo.json().catch(() => null), 'copia');
      const enlace = el('a', { href: URL.createObjectURL(await archivo.blob()), download: 'historia-clinica.json' });
      enlace.click();
      URL.revokeObjectURL(enlace.href);
      estado.append(aviso('ok', 'Descargaste una copia. Queda registrada como entrega de tu historia (Ley 26.529).'));
    } catch (error) {
      estado.append(aviso('error', motivo(error)));
    }
  }));

  if (!respuesta.tieneFicha || !respuesta.historia) {
    vaciar(contenido, aviso('info', el('p', {}, 'Todavía no hay registros clínicos en tu historia.')));
    return;
  }
  const historia = respuesta.historia;
  vaciar(contenido,
    el('div', { class: 'fila fila--entre' },
      insignia(`Versión ${historia.historiaVersion}`, 'marca'), descargar),
    estado,
    tarjetaOdontograma(historia),
    tarjetaEvoluciones(historia),
    el('div', { class: 'rejilla rejilla--2' }, tarjetaAnamnesis(historia), tarjetaConsentimientos(historia)));
}

function tarjetaOdontograma(historia: HistoriaClinica): HTMLElement {
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'odo' },
    el('h2', { id: 'odo', style: 'margin: 0; font-size: var(--t-lg)' }, 'Odontograma (FDI)'),
    historia.odontograma.length
      ? el('table', { class: 'tabla' },
        el('thead', {}, el('tr', {}, ...['Pieza', 'Cara', 'Estado', 'Fecha'].map((t) => el('th', { scope: 'col' }, t)))),
        el('tbody', {}, ...historia.odontograma.map((r) => el('tr', {},
          el('td', { 'data-etiqueta': 'Pieza' }, `${r.piezaFdi}${r.temporal ? ' (temporal)' : ''}`),
          el('td', { 'data-etiqueta': 'Cara' }, r.cara ?? 'Pieza completa'),
          el('td', { 'data-etiqueta': 'Estado' }, r.estado),
          el('td', { 'data-etiqueta': 'Fecha' }, fechaHora(r.fecha))))))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin registros.'));
}

function tarjetaEvoluciones(historia: HistoriaClinica): HTMLElement {
  const evoluciones = [...historia.evoluciones].reverse();
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'evo' },
    el('h2', { id: 'evo', style: 'margin: 0; font-size: var(--t-lg)' }, 'Evoluciones'),
    evoluciones.length
      ? el('ol', { class: 'linea-tiempo' }, ...evoluciones.map((e) => el('li', {},
        el('div', { class: 'metrica__pie' }, `${fechaHora(e.fecha)}${e.version > 1 ? ` · versión ${e.version}` : ''}`),
        el('p', {}, e.texto))))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Todavía no hay evoluciones.'));
}

function tarjetaAnamnesis(historia: HistoriaClinica): HTMLElement {
  const a = historia.anamnesis;
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'ana' },
    el('h2', { id: 'ana', style: 'margin: 0; font-size: var(--t-lg)' }, 'Anamnesis'),
    a
      ? el('div', {},
        el('dl', { style: 'display: grid; grid-template-columns: auto 1fr; gap: var(--e-1) var(--e-3); margin: 0' },
          ...Object.entries(a.respuestas).flatMap(([clave, valor]) => [
            el('dt', { class: 'campo__ayuda' }, clave.replaceAll('_', ' ')), el('dd', { style: 'margin: 0' }, String(valor))])),
        a.textoLibre ? el('p', { style: 'margin-top: var(--e-3)' }, a.textoLibre) : null,
        el('p', { class: 'metrica__pie' }, `Actualizada ${fechaHora(a.fecha)}`))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin anamnesis registrada.'));
}

function tarjetaConsentimientos(historia: HistoriaClinica): HTMLElement {
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'con' },
    el('h2', { id: 'con', style: 'margin: 0; font-size: var(--t-lg)' }, 'Consentimientos'),
    historia.consentimientos.length
      ? el('ul', { style: 'margin: 0; padding-left: 1.1rem' }, ...historia.consentimientos.map((c) =>
        el('li', {}, el('strong', {}, c.tipo.replaceAll('_', ' ')), ` · versión ${c.version} · ${fechaHora(c.fecha)}`)))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin consentimientos registrados.'));
}
