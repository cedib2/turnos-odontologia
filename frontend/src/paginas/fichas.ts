// Ficha clínica (C-11/C-12) con búsqueda por paciente. La lectura respeta el ámbito del rol: el odontólogo
// ve solo fichas de pacientes que atiende; el administrador, todas.

import { api, ApiError } from '../api.js';
import { fechaHora } from '../fechas.js';
import { abrirDialogo, mensajeStaff } from '../formularios.js';
import { montarLayout } from '../layout.js';
import type { HistoriaClinica, Paciente, Profesional, RegistroOdontograma } from '../tipos.js';
import { $, aviso, campo, conCarga, el, insignia, opciones, toast, vaciar } from '../ui.js';

montarLayout('fichas');
const main = $('#contenido');

let pacientes: Paciente[] = [];
let profesionales: Profesional[] = [];
let seleccionado: Paciente | null = null;

const buscador = el('input', {
  class: 'control', type: 'search', placeholder: 'Apellido, nombre o documento', autocomplete: 'off',
  'aria-controls': 'lista-pacientes',
});
const lista = el('ul', { class: 'lista-pacientes', id: 'lista-pacientes', 'aria-label': 'Pacientes' });
const detalle = el('section', { class: 'pila', 'aria-live': 'polite', tabindex: '-1' });

vaciar(main,
  el('div', { class: 'titulo-pagina' },
    el('h1', {}, 'Fichas clínicas'),
    el('p', {}, 'Buscá un paciente para ver su anamnesis, odontograma, evoluciones y consentimientos. La historia se versiona: nada se sobrescribe.')),
  el('div', { class: 'ficha-layout' },
    el('aside', { class: 'tarjeta pila', 'aria-label': 'Búsqueda de pacientes' },
      campo('buscar', 'Buscar paciente', buscador), lista),
    detalle));
vaciar(detalle, el('p', { class: 'vacio' }, 'Elegí un paciente de la lista.'));

const normalizar = (texto: string) => texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();

function pintarLista(): void {
  const consulta = normalizar(buscador.value.trim());
  const filtrados = pacientes.filter((p) =>
    !consulta || normalizar(`${p.apellido} ${p.nombre} ${p.documento}`).includes(consulta));
  if (!filtrados.length) {
    vaciar(lista, el('li', { class: 'campo__ayuda' }, consulta ? 'Ningún paciente coincide.' : 'No hay pacientes cargados.'));
    return;
  }
  vaciar(lista, ...filtrados.map((p) => el('li', {}, el('button', {
    type: 'button', 'aria-current': seleccionado?.id === p.id ? 'true' : null,
    onclick: () => { seleccionado = p; pintarLista(); void cargarFicha(); },
  }, el('strong', {}, `${p.apellido}, ${p.nombre}`), el('div', { class: 'metrica__pie' }, `DNI ${p.documento}`)))));
}
buscador.addEventListener('input', pintarLista);

async function cargarFicha(): Promise<void> {
  if (!seleccionado) return;
  const paciente = seleccionado;
  vaciar(detalle, el('p', { class: 'campo__ayuda' }, 'Cargando ficha…'));
  try {
    const historia = await api.get<HistoriaClinica>('/fichas', { pacienteId: paciente.id });
    pintarFicha(paciente, historia);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      const abrir = el('button', { class: 'btn', type: 'button' }, 'Abrir ficha clínica');
      abrir.addEventListener('click', () => conCarga(abrir, async () => {
        try {
          await api.post('/fichas', { pacienteId: paciente.id });
          toast('Ficha abierta.');
          await cargarFicha();
        } catch (falla) {
          vaciar(detalle, encabezado(paciente), aviso('error', mensajeStaff(falla)));
        }
      }));
      vaciar(detalle, encabezado(paciente),
        el('section', { class: 'tarjeta pila' }, el('p', { style: 'margin: 0' }, 'Este paciente todavía no tiene ficha clínica.'), abrir));
      return;
    }
    vaciar(detalle, encabezado(paciente), aviso('error', mensajeStaff(error)));
  }
  detalle.focus();
}

function encabezado(paciente: Paciente): HTMLElement {
  return el('div', { class: 'tarjeta__titulo' },
    el('h2', { style: 'margin: 0' }, `${paciente.apellido}, ${paciente.nombre}`),
    el('span', { class: 'campo__ayuda' }, `DNI ${paciente.documento}`));
}

function pintarFicha(paciente: Paciente, historia: HistoriaClinica): void {
  vaciar(detalle,
    el('div', { class: 'fila fila--entre' }, encabezado(paciente),
      el('div', { class: 'fila' },
        insignia(`Versión ${historia.historiaVersion}`, 'marca'),
        el('button', { class: 'btn btn--secundario btn--chico', type: 'button', onclick: () => dialogoEvolucion(historia) }, 'Nueva evolución'))),
    tarjetaOdontograma(historia),
    tarjetaEvoluciones(historia),
    el('div', { class: 'rejilla rejilla--2' }, tarjetaAnamnesis(historia), tarjetaConsentimientos(historia)));
}

// ---------- Odontograma FDI ----------

// Cuadrantes FDI en el orden en que se ven de frente: superior derecho (18→11), superior izquierdo (21→28), etc.
const PERMANENTES = { sup: [[18, 17, 16, 15, 14, 13, 12, 11], [21, 22, 23, 24, 25, 26, 27, 28]],
  inf: [[48, 47, 46, 45, 44, 43, 42, 41], [31, 32, 33, 34, 35, 36, 37, 38]] };
const TEMPORALES = { sup: [[55, 54, 53, 52, 51], [61, 62, 63, 64, 65]],
  inf: [[85, 84, 83, 82, 81], [71, 72, 73, 74, 75]] };
const CARAS = ['oclusal', 'mesial', 'distal', 'vestibular', 'lingual', 'palatino'];
const ESTADOS = ['sano', 'caries', 'obturado', 'endodoncia', 'corona', 'implante', 'ausente', 'a extraer'];
const ATENCION = new Set(['caries', 'a extraer', 'fractura']);

function tarjetaOdontograma(historia: HistoriaClinica): HTMLElement {
  const porPieza = new Map<number, RegistroOdontograma[]>();
  for (const registro of historia.odontograma) {
    porPieza.set(registro.piezaFdi, [...(porPieza.get(registro.piezaFdi) ?? []), registro]);
  }
  const pieza = (codigo: number) => {
    const registros = porPieza.get(codigo) ?? [];
    const estados = registros.map((r) => r.estado.toLowerCase());
    const clase = !registros.length ? 'pieza'
      : estados.includes('ausente') ? 'pieza pieza--ausente'
        : estados.some((e) => ATENCION.has(e)) ? 'pieza pieza--atencion' : 'pieza pieza--registrada';
    const resumen = registros.map((r) => `${r.cara ?? 'pieza completa'}: ${r.estado}`).join(', ');
    return el('button', {
      class: clase, type: 'button',
      'aria-label': `Pieza ${codigo}${resumen ? `: ${resumen}` : ', sin registros'}`,
      title: resumen || 'Sin registros', onclick: () => dialogoOdontograma(historia, codigo),
    }, String(codigo), registros.length ? el('small', {}, registros[0]!.estado) : null);
  };
  const arcada = (filas: number[][]) => el('div', { class: 'arcada' },
    el('div', { class: 'hemi hemi--der' }, ...filas[0]!.map(pieza)),
    el('div', { class: 'hemi' }, ...filas[1]!.map(pieza)));

  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'odo-titulo' },
    el('div', { class: 'tarjeta__titulo' },
      el('h3', { id: 'odo-titulo' }, 'Odontograma (FDI)'),
      el('button', { class: 'btn btn--secundario btn--chico', type: 'button', onclick: () => dialogoOdontograma(historia, null) }, 'Registrar')),
    el('div', { class: 'odontograma' },
      el('span', { class: 'odontograma__titulo' }, 'Permanentes'),
      arcada(PERMANENTES.sup), arcada(PERMANENTES.inf),
      el('span', { class: 'odontograma__titulo' }, 'Temporales'),
      arcada(TEMPORALES.sup), arcada(TEMPORALES.inf)),
    el('div', { class: 'leyenda' },
      el('span', { class: 'l-registrada' }, 'Con registro'),
      el('span', { class: 'l-atencion' }, 'Requiere atención'),
      el('span', { class: 'l-ausente' }, 'Ausente')),
    historia.odontograma.length
      ? el('details', { class: 'detalle' }, el('summary', {}, `Registros vigentes (${historia.odontograma.length})`),
        el('table', { class: 'tabla' },
          el('thead', {}, el('tr', {}, ...['Pieza', 'Cara', 'Estado', 'Versión', 'Registró'].map((t) => el('th', { scope: 'col' }, t)))),
          el('tbody', {}, ...historia.odontograma.map((r) => el('tr', {},
            el('td', { 'data-etiqueta': 'Pieza' }, `${r.piezaFdi}${r.temporal ? ' (temporal)' : ''}`),
            el('td', { 'data-etiqueta': 'Cara' }, r.cara ?? '—'),
            el('td', { 'data-etiqueta': 'Estado' }, r.estado),
            el('td', { 'data-etiqueta': 'Versión' }, String(r.version)),
            el('td', { 'data-etiqueta': 'Registró' }, `${r.autor} · ${fechaHora(r.fecha)}`))))))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin registros. Tocá una pieza para registrar su estado.'));
}

function dialogoOdontograma(historia: HistoriaClinica, codigo: number | null): void {
  const todas = [...PERMANENTES.sup.flat(), ...PERMANENTES.inf.flat(), ...TEMPORALES.sup.flat(), ...TEMPORALES.inf.flat()];
  const pieza = el('select', { class: 'control' });
  opciones(pieza, todas.map((c) => ({ valor: String(c), texto: `${c}${c >= 51 ? ' (temporal)' : ''}` })), 'Elegí una pieza');
  pieza.value = codigo ? String(codigo) : '';
  const cara = el('select', { class: 'control' });
  opciones(cara, CARAS.map((c) => ({ valor: c, texto: c })), 'Pieza completa');
  const estado = el('input', { class: 'control', type: 'text', list: 'estados-odontograma', maxlength: 60, required: true });
  const sugerencias = el('datalist', { id: 'estados-odontograma' }, ...ESTADOS.map((e) => el('option', { value: e })));
  abrirDialogo({
    titulo: codigo ? `Pieza ${codigo}` : 'Registrar en el odontograma',
    descripcion: 'Cada registro crea una versión nueva; el historial anterior se conserva.',
    campos: [
      el('div', { class: 'formulario__fila' }, campo('o-pieza', 'Pieza (FDI)', pieza), campo('o-cara', 'Cara', cara)),
      campo('o-estado', 'Estado', estado, 'Elegí una sugerencia o escribí el hallazgo.'), sugerencias,
    ],
    accion: 'Registrar',
    validar: () => (!pieza.value || !estado.value.trim() ? 'Elegí la pieza y completá el estado.' : null),
    enviar: async () => {
      await api.post(`/fichas/${historia.fichaId}/odontograma`, {
        piezaFdi: Number(pieza.value), cara: cara.value || null, estado: estado.value.trim(),
      });
      toast('Registro guardado.');
      await cargarFicha();
    },
  });
}

// ---------- Evoluciones, anamnesis y consentimientos ----------

const nombreProfesional = (id: number) => {
  const p = profesionales.find((x) => x.id === id);
  return p ? `${p.nombre} ${p.apellido}` : `Profesional #${id}`;
};

function tarjetaEvoluciones(historia: HistoriaClinica): HTMLElement {
  const evoluciones = [...historia.evoluciones].reverse();
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'evo-titulo' },
    el('h3', { id: 'evo-titulo' }, 'Evoluciones'),
    evoluciones.length
      ? el('ol', { class: 'linea-tiempo' }, ...evoluciones.map((e) => el('li', {},
        el('div', { class: 'metrica__pie' }, `${fechaHora(e.fecha)} · ${nombreProfesional(e.profesionalId)}${e.version > 1 ? ` · versión ${e.version}` : ''}`),
        el('p', {}, e.texto))))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Todavía no hay evoluciones.'));
}

function tarjetaAnamnesis(historia: HistoriaClinica): HTMLElement {
  const a = historia.anamnesis;
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'ana-titulo' },
    el('h3', { id: 'ana-titulo' }, 'Anamnesis'),
    a
      ? el('div', {},
        el('dl', { style: 'display: grid; grid-template-columns: auto 1fr; gap: var(--e-1) var(--e-3); margin: 0' },
          ...Object.entries(a.respuestas).flatMap(([clave, valor]) => [
            el('dt', { class: 'campo__ayuda' }, clave), el('dd', { style: 'margin: 0' }, String(valor))])),
        a.textoLibre ? el('p', { style: 'margin-top: var(--e-3)' }, a.textoLibre) : null,
        el('p', { class: 'metrica__pie' }, `Versión ${a.version} · ${a.autor} · ${fechaHora(a.fecha)}`))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin anamnesis registrada.'));
}

function tarjetaConsentimientos(historia: HistoriaClinica): HTMLElement {
  return el('section', { class: 'tarjeta pila', 'aria-labelledby': 'con-titulo' },
    el('h3', { id: 'con-titulo' }, 'Consentimientos'),
    historia.consentimientos.length
      ? el('ul', { style: 'margin: 0; padding-left: 1.1rem' }, ...historia.consentimientos.map((c) =>
        el('li', {}, el('strong', {}, c.tipo), ` · versión ${c.version} · ${fechaHora(c.fecha)}`)))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin consentimientos registrados.'));
}

function dialogoEvolucion(historia: HistoriaClinica): void {
  const profesional = el('select', { class: 'control' });
  opciones(profesional, profesionales.map((p) => ({ valor: String(p.id), texto: `${p.nombre} ${p.apellido}` })), 'Elegí el profesional');
  const texto = el('textarea', { class: 'control', rows: 5, maxlength: 4000, required: true });
  abrirDialogo({
    titulo: 'Nueva evolución',
    descripcion: 'Queda registrada con autor y fecha; para corregirla se crea una versión nueva.',
    campos: [campo('ev-profesional', 'Profesional', profesional), campo('ev-texto', 'Evolución', texto)],
    accion: 'Guardar evolución',
    validar: () => (!profesional.value || !texto.value.trim() ? 'Elegí el profesional y escribí la evolución.' : null),
    enviar: async () => {
      await api.post(`/fichas/${historia.fichaId}/evoluciones`, {
        profesionalId: Number(profesional.value), turnoId: null, texto: texto.value.trim(), reemplazaId: null,
      });
      toast('Evolución guardada.');
      await cargarFicha();
    },
  });
}

// ---------- Inicio ----------

const [ps, pr] = await Promise.allSettled([api.get<Paciente[]>('/pacientes'), api.get<Profesional[]>('/profesionales')]);
pacientes = ps.status === 'fulfilled' ? ps.value : [];
profesionales = pr.status === 'fulfilled' ? pr.value : [];
if (ps.status === 'rejected') {
  vaciar(lista, el('li', {}, aviso('error', mensajeStaff(ps.reason))));
} else {
  pintarLista();
  const pedido = Number(new URLSearchParams(location.search).get('paciente'));
  const inicial = pacientes.find((p) => p.id === pedido);
  if (inicial) { seleccionado = inicial; pintarLista(); void cargarFicha(); }
}
