// Cumplimiento (Parte 4): métricas con metodología (C-22), dossier de evidencia (C-18) y
// exportaciones, respaldos y restore aislado (C-17).

import { api, ApiError } from '../api.js';
import { fechaHora, fechaLocal, instanteLocal, sumarDias } from '../fechas.js';
import { abrirDialogo, mensajeStaff } from '../formularios.js';
import { montarLayout } from '../layout.js';
import type { Dossier, ItemDossier, Metrica, OperacionCumplimiento, Paciente } from '../tipos.js';
import { $, aviso, campo, conCarga, el, insignia, opciones, porcentaje, toast, vaciar } from '../ui.js';

montarLayout('cumplimiento');
const main = $('#contenido');

type Pestana = 'metricas' | 'dossier' | 'operaciones';
const PESTANAS: { id: Pestana; texto: string }[] = [
  { id: 'metricas', texto: 'Métricas' },
  { id: 'dossier', texto: 'Dossier' },
  { id: 'operaciones', texto: 'Exportación y respaldo' },
];
let actual: Pestana = (PESTANAS.find((p) => p.id === location.hash.slice(1))?.id) ?? 'metricas';

const lista = el('div', { class: 'pestanas', role: 'tablist', 'aria-label': 'Secciones de cumplimiento' });
const panel = el('section', { role: 'tabpanel', tabindex: '-1' });

vaciar(main,
  el('div', { class: 'titulo-pagina' },
    el('h1', {}, 'Cumplimiento'),
    el('p', {}, 'Cada cifra sale con su definición, fórmula, fuente y período. Cada consulta y operación queda en el log de auditoría.')),
  lista, panel);

function pintarPestanas(): void {
  vaciar(lista, ...PESTANAS.map((p) => {
    const boton = el('button', {
      role: 'tab', type: 'button', id: `tab-${p.id}`, 'aria-selected': String(p.id === actual),
      'aria-controls': 'panel', tabindex: p.id === actual ? '0' : '-1',
    }, p.texto);
    boton.addEventListener('click', () => { actual = p.id; history.replaceState(null, '', `#${p.id}`); void mostrar(); });
    boton.addEventListener('keydown', (evento) => {
      const indice = PESTANAS.findIndex((x) => x.id === actual);
      const delta = evento.key === 'ArrowRight' ? 1 : evento.key === 'ArrowLeft' ? -1 : 0;
      if (!delta) return;
      actual = PESTANAS[(indice + delta + PESTANAS.length) % PESTANAS.length]!.id;
      void mostrar().then(() => document.getElementById(`tab-${actual}`)?.focus());
    });
    return boton;
  }));
  panel.id = 'panel';
  panel.setAttribute('aria-labelledby', `tab-${actual}`);
}

async function mostrar(): Promise<void> {
  pintarPestanas();
  if (actual === 'metricas') pintarMetricas();
  else if (actual === 'dossier') await pintarDossier();
  else pintarOperaciones();
}

// ---------- Métricas (C-22) ----------

const ESTADOS_METRICA: Record<string, { texto: string; tipo: 'ok' | 'alerta' | 'info' | '' }> = {
  CALCULADA: { texto: 'Calculada', tipo: 'ok' },
  SIN_DATOS: { texto: 'Sin datos', tipo: '' },
  NO_DISPONIBLE: { texto: 'No disponible', tipo: 'alerta' },
  DIFERIDA: { texto: 'Diferida', tipo: 'info' },
};

const NOMBRES_METRICA: Record<string, string> = {
  ADOPCION_ONLINE: 'Adopción de la reserva online',
  CUMPLIMIENTO_VENTANA_48H: 'Cambios con 48 hs hábiles',
  CONFIRMACIONES: 'Confirmaciones por WhatsApp',
  OCUPACION_POR_SILLON: 'Ocupación por sillón',
  AUSENTISMO: 'Ausentismo',
  EVIDENCIA_CUMPLIMIENTO: 'Evidencia de cumplimiento',
};

let desde = sumarDias(fechaLocal(), -30);
let hasta = fechaLocal();

function pintarMetricas(): void {
  const campoDesde = el('input', { class: 'control', type: 'date', value: desde });
  const campoHasta = el('input', { class: 'control', type: 'date', value: hasta });
  const consultar = el('button', { class: 'btn', type: 'submit' }, 'Consultar');
  const resultado = el('div', { 'aria-live': 'polite' });
  const formulario = el('form', { class: 'tarjeta' },
    el('div', { class: 'fila' }, campo('m-desde', 'Desde', campoDesde), campo('m-hasta', 'Hasta (inclusive)', campoHasta), consultar));

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (!campoDesde.value || !campoHasta.value || campoHasta.value < campoDesde.value) {
      vaciar(resultado, aviso('error', 'Elegí un período válido: la fecha de fin no puede ser anterior a la de inicio.'));
      return;
    }
    desde = campoDesde.value;
    hasta = campoHasta.value;
    void conCarga(consultar, () => cargarMetricas(resultado));
  });
  vaciar(panel, el('div', { class: 'pila' }, formulario, resultado));
  void cargarMetricas(resultado);
}

async function cargarMetricas(resultado: HTMLElement): Promise<void> {
  vaciar(resultado, el('p', { class: 'campo__ayuda' }, 'Calculando…'));
  try {
    const metricas = await api.get<Metrica[]>('/metricas', {
      desde: instanteLocal(desde, '00:00'), hasta: instanteLocal(sumarDias(hasta, 1), '00:00'), motivo: 'tablero',
    });
    vaciar(resultado, el('div', { class: 'rejilla rejilla--2' }, ...metricas.map(tarjetaMetrica)));
  } catch (error) {
    vaciar(resultado, aviso('error', mensajeStaff(error)));
  }
}

function tarjetaMetrica(metrica: Metrica): HTMLElement {
  const estado = ESTADOS_METRICA[metrica.estado] ?? { texto: metrica.estado, tipo: '' as const };
  const conValor = metrica.valor !== null;
  const porSillon = Array.isArray(metrica.detalle['porSillonYMes']) ? metrica.detalle['porSillonYMes'] as Record<string, unknown>[] : [];
  return el('article', { class: 'tarjeta pila' },
    el('div', { class: 'tarjeta__titulo' },
      el('h3', {}, NOMBRES_METRICA[metrica.codigo] ?? metrica.codigo),
      insignia(estado.texto, estado.tipo)),
    conValor
      ? el('div', {},
        el('p', { class: 'metrica__valor', style: 'margin: 0' }, porcentaje(metrica.valor)),
        el('div', { class: 'barra', role: 'img', 'aria-label': porcentaje(metrica.valor) },
          el('span', { style: `width: ${Math.min(100, Math.max(0, metrica.valor ?? 0))}%` })),
        metrica.numerador !== null
          ? el('p', { class: 'metrica__pie', style: 'margin-top: var(--e-2)' }, `${metrica.numerador} de ${metrica.denominador}`)
          : null)
      : porSillon.length
        ? el('ul', { style: 'margin: 0; padding-left: 1.1rem' }, ...porSillon.map((fila) =>
          el('li', {}, `Sillón ${String(fila['sillonId'])} · ${String(fila['periodo'])}: ${porcentaje(Number(fila['valor']))}`)))
        : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Sin valor publicable para este período.'),
    el('p', { style: 'margin: 0; font-size: var(--t-sm)' }, metrica.definicion),
    el('p', { class: 'metrica__pie', style: 'margin: 0' }, `Meta: ${metrica.meta}`),
    el('details', { class: 'detalle' },
      el('summary', {}, 'Metodología'),
      el('p', {}, el('strong', {}, 'Fórmula: '), metrica.formula),
      el('p', {}, el('strong', {}, 'Fuente: '), metrica.fuente),
      el('p', {}, el('strong', {}, 'Período: '), `${fechaHora(metrica.periodoDesde)} a ${fechaHora(metrica.periodoHasta)} (${metrica.zonaHoraria})`),
      Object.keys(metrica.detalle).length ? el('pre', {}, JSON.stringify(metrica.detalle, null, 2)) : null));
}

// ---------- Dossier (C-18) ----------

async function pintarDossier(): Promise<void> {
  vaciar(panel, el('p', { class: 'campo__ayuda' }, 'Generando dossier…'));
  let dossier: Dossier;
  try {
    dossier = await api.get<Dossier>('/cumplimiento/dossier', { motivo: 'consulta' });
  } catch (error) {
    vaciar(panel, aviso('error', mensajeStaff(error)));
    return;
  }
  const c = dossier.completitud;
  const publicar = el('button', { class: 'btn', type: 'button' }, 'Publicar dossier');
  publicar.addEventListener('click', () => conCarga(publicar, async () => {
    try {
      await api.post('/cumplimiento/dossier/publicacion?motivo=publicacion');
      toast('Dossier publicado.');
      await pintarDossier();
    } catch (error) {
      const pendientes = error instanceof ApiError && error.status === 409 && error.cuerpo && typeof error.cuerpo === 'object'
        ? ((error.cuerpo as { pendientes?: string[] }).pendientes ?? []) : [];
      toast(pendientes.length ? `No se puede publicar: faltan ${pendientes.length} evidencias.` : mensajeStaff(error), 'error');
    }
  }));

  vaciar(panel, el('div', { class: 'pila' },
    el('section', { class: 'tarjeta pila' },
      el('div', { class: 'tarjeta__titulo' },
        el('h2', {}, 'Completitud'),
        insignia(dossier.publicable ? 'Publicable' : 'Con pendientes', dossier.publicable ? 'ok' : 'alerta')),
      el('p', { class: 'metrica__valor', style: 'margin: 0' }, porcentaje(c.porcentaje)),
      el('div', { class: 'barra' }, el('span', { style: `width: ${c.porcentaje ?? 0}%` })),
      el('p', { class: 'metrica__pie' }, `${c.presentes} de ${c.exigidos} evidencias obligatorias · generado ${fechaHora(dossier.generadoAt)}`),
      el('details', { class: 'detalle' }, el('summary', {}, 'Metodología'), el('p', {}, c.metodologia), el('p', {}, c.formula)),
      el('div', { class: 'fila' }, publicar,
        el('button', { class: 'btn btn--secundario', type: 'button', onclick: () => dialogoSubprocesador() }, 'Registrar subprocesador'))),
    el('section', { class: 'tarjeta' },
      el('h2', {}, 'Evidencias'),
      el('table', { class: 'tabla' },
        el('thead', {}, el('tr', {}, ...['Ítem', 'Base normativa', 'Fuente', 'Estado', 'Evidencia', ''].map((t) => el('th', { scope: 'col' }, t)))),
        el('tbody', {}, ...dossier.items.map(filaItem)))),
    dossier.subprocesadoresIncluidos.length || dossier.subprocesadoresExcluidos.length
      ? el('section', { class: 'tarjeta' },
        el('h2', {}, 'Subprocesadores'),
        el('ul', {}, ...dossier.subprocesadoresIncluidos.map((s) => el('li', {}, `${s.nombre} (${s.servicio}) `, insignia('con cláusulas', 'ok'))),
          ...dossier.subprocesadoresExcluidos.map((s) => el('li', {}, `${s.nombre} (${s.servicio}) `, insignia('sin cláusulas AAIP 198/2023', 'error')))))
      : null));
}

function filaItem(item: ItemDossier): HTMLElement {
  const presente = item.estado === 'PRESENTE';
  return el('tr', {},
    el('td', { 'data-etiqueta': 'Ítem' }, el('strong', {}, item.descripcion), el('div', { class: 'metrica__pie mono' }, item.codigo)),
    el('td', { 'data-etiqueta': 'Base' }, item.baseNormativa),
    el('td', { 'data-etiqueta': 'Fuente' }, item.fuente === 'MANUAL' ? 'Manual' : 'Automática'),
    el('td', { 'data-etiqueta': 'Estado' }, insignia(presente ? 'Presente' : 'Pendiente', presente ? 'ok' : 'alerta')),
    el('td', { 'data-etiqueta': 'Evidencia' }, item.evidencia ?? '—',
      item.verificadoPor ? el('div', { class: 'metrica__pie' }, `${item.verificadoPor} · ${fechaHora(item.verificadoAt)}`) : null),
    el('td', { 'data-etiqueta': '' }, item.fuente === 'MANUAL'
      ? el('button', { class: 'btn btn--secundario btn--chico', type: 'button', onclick: () => dialogoEvidencia(item) }, presente ? 'Actualizar' : 'Registrar')
      : null));
}

function dialogoEvidencia(item: ItemDossier): void {
  const referencia = el('input', { class: 'control', type: 'text', maxlength: 500, placeholder: 'Ej.: Acta 12/2026, expediente AAIP…' });
  const motivo = el('input', { class: 'control', type: 'text', maxlength: 200 });
  abrirDialogo({
    titulo: item.descripcion,
    descripcion: `Base normativa: ${item.baseNormativa}. Registrá la referencia del documento que lo acredita.`,
    campos: [campo('ev-ref', 'Referencia documental', referencia), campo('ev-motivo', 'Motivo (opcional)', motivo)],
    accion: 'Registrar evidencia',
    validar: () => (referencia.value.trim() ? null : 'La referencia es obligatoria.'),
    enviar: async () => {
      await api.put(`/cumplimiento/dossier/items/${encodeURIComponent(item.codigo)}/evidencia`, {
        referencia: referencia.value.trim(), motivo: motivo.value.trim() || null,
      });
      toast('Evidencia registrada.');
      await pintarDossier();
    },
  });
}

function dialogoSubprocesador(): void {
  const nombre = el('input', { class: 'control', type: 'text', maxlength: 120 });
  const servicio = el('input', { class: 'control', type: 'text', maxlength: 120, placeholder: 'Ej.: mensajería, pagos, hosting' });
  const ubicacion = el('input', { class: 'control', type: 'text', maxlength: 120, placeholder: 'Ej.: Argentina' });
  const clausulas = el('input', { class: 'control', type: 'text', maxlength: 300 });
  const dpa = el('input', { class: 'control', type: 'text', maxlength: 300 });
  abrirDialogo({
    titulo: 'Registrar subprocesador',
    descripcion: 'Sin referencia a cláusulas AAIP 198/2023 queda registrado pero excluido del dossier.',
    campos: [
      el('div', { class: 'formulario__fila' }, campo('sp-nombre', 'Nombre', nombre), campo('sp-servicio', 'Servicio', servicio)),
      campo('sp-ubicacion', 'Ubicación de los datos', ubicacion),
      campo('sp-clausulas', 'Referencia a cláusulas AAIP 198/2023', clausulas),
      campo('sp-dpa', 'Referencia al DPA', dpa),
    ],
    accion: 'Registrar',
    validar: () => (nombre.value.trim() && servicio.value.trim() ? null : 'Nombre y servicio son obligatorios.'),
    enviar: async () => {
      await api.post('/cumplimiento/dossier/subprocesadores', {
        nombre: nombre.value.trim(), servicio: servicio.value.trim(), ubicacionDatos: ubicacion.value.trim() || null,
        clausulasAaipReferencia: clausulas.value.trim() || null, dpaReferencia: dpa.value.trim() || null,
      });
      toast('Subprocesador registrado.');
      await pintarDossier();
    },
  });
}

// ---------- Exportación, respaldo y restore (C-17) ----------

const operaciones: OperacionCumplimiento[] = [];

function pintarOperaciones(): void {
  const historial = el('div', { 'aria-live': 'polite' });
  const vencidas = el('div', {});
  const exportarBtn = el('button', { class: 'btn', type: 'button', onclick: () => void dialogoExportacion(historial) }, 'Nueva exportación');
  const respaldarBtn = el('button', { class: 'btn btn--secundario', type: 'button' }, 'Respaldar ahora');
  respaldarBtn.addEventListener('click', () => conCarga(respaldarBtn, async () => {
    try {
      const operacion = await api.post<OperacionCumplimiento>('/cumplimiento/respaldos', { motivo: 'respaldo manual' });
      registrar(operacion, historial);
      toast(operacion.estado === 'COMPLETADA' ? 'Respaldo completado.' : `Respaldo ${operacion.estado.toLowerCase()}.`, operacion.estado === 'FALLIDA' ? 'error' : 'normal');
    } catch (error) {
      toast(mensajeStaff(error), 'error');
    }
  }));

  vaciar(panel, el('div', { class: 'pila' },
    el('section', { class: 'tarjeta pila' },
      el('h2', {}, 'Operaciones'),
      el('p', { class: 'campo__ayuda' }, 'Las exportaciones y respaldos se cifran en reposo (AES-256-GCM). El restore se prueba en una base aislada y se verifica fila por fila antes de darlo por bueno.'),
      el('div', { class: 'fila' }, exportarBtn, respaldarBtn)),
    el('section', { class: 'tarjeta' }, el('h2', {}, 'Esta sesión'), historial),
    el('section', { class: 'tarjeta' }, el('h2', {}, 'Exportaciones vencidas'), vencidas)));
  pintarHistorial(historial);
  void cargarVencidas(vencidas);
}

function registrar(operacion: OperacionCumplimiento, historial: HTMLElement): void {
  const indice = operaciones.findIndex((o) => o.id === operacion.id);
  if (indice >= 0) operaciones.splice(indice, 1);
  operaciones.unshift(operacion);
  pintarHistorial(historial);
}

function insigniaOperacion(estado: string): HTMLElement {
  return insignia(estado.toLowerCase(), estado === 'COMPLETADA' ? 'ok' : estado === 'FALLIDA' ? 'error' : 'info');
}

function pintarHistorial(historial: HTMLElement): void {
  if (!operaciones.length) {
    vaciar(historial, el('p', { class: 'campo__ayuda' }, 'Todavía no hiciste operaciones en esta sesión.'));
    return;
  }
  vaciar(historial, el('table', { class: 'tabla' },
    el('thead', {}, el('tr', {}, ...['#', 'Tipo', 'Estado', 'Detalle', ''].map((t) => el('th', { scope: 'col' }, t)))),
    el('tbody', {}, ...operaciones.map((o) => el('tr', {},
      el('td', { 'data-etiqueta': '#' }, String(o.id)),
      el('td', { 'data-etiqueta': 'Tipo' }, `${o.tipo.toLowerCase()} ${o.alcance ? `· ${o.alcance.toLowerCase()}` : ''}`),
      el('td', { 'data-etiqueta': 'Estado' }, insigniaOperacion(o.estado)),
      el('td', { 'data-etiqueta': 'Detalle' },
        o.error ? el('span', { style: 'color: var(--c-error)' }, o.error)
          : `${o.tablas ?? '—'} tablas · ${o.filas ?? '—'} filas · ${fechaHora(o.completadoAt ?? o.solicitadoAt)}`,
        o.checksumSha256 ? el('div', { class: 'metrica__pie mono' }, `sha256 ${o.checksumSha256.slice(0, 16)}…`) : null),
      el('td', { 'data-etiqueta': '' }, accionesOperacion(o, historial)))))));
}

function accionesOperacion(o: OperacionCumplimiento, historial: HTMLElement): HTMLElement | null {
  if (o.estado !== 'COMPLETADA') return null;
  if (o.tipo === 'EXPORTACION') {
    return el('a', {
      class: 'btn btn--secundario btn--chico', download: `exportacion-${o.id}.json`,
      href: `/cumplimiento/exportaciones/${o.id}/contenido?motivo=${encodeURIComponent('descarga desde el panel')}`,
    }, 'Descargar');
  }
  if (o.tipo === 'RESPALDO') {
    const boton = el('button', { class: 'btn btn--secundario btn--chico', type: 'button' }, 'Probar restore');
    boton.addEventListener('click', () => conCarga(boton, async () => {
      try {
        const restore = await api.post<OperacionCumplimiento>(`/cumplimiento/respaldos/${o.id}/restore-aislado`, { motivo: 'prueba de restore' });
        registrar(restore, historial);
        toast(restore.estado === 'COMPLETADA' ? 'Restore verificado en el entorno aislado.' : 'El restore falló.', restore.estado === 'COMPLETADA' ? 'normal' : 'error');
      } catch (error) {
        toast(mensajeStaff(error), 'error');
      }
    }));
    return boton;
  }
  return null;
}

async function cargarVencidas(nodo: HTMLElement): Promise<void> {
  try {
    const vencidas = await api.get<OperacionCumplimiento[]>('/cumplimiento/exportaciones/vencidas');
    vaciar(nodo, vencidas.length
      ? el('ul', {}, ...vencidas.map((o) => el('li', {}, `#${o.id} · ${o.alcance} · venció ${fechaHora(o.plazoVenceAt)}`)))
      : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'No hay solicitudes fuera de plazo.'));
  } catch (error) {
    vaciar(nodo, aviso('error', mensajeStaff(error)));
  }
}

async function dialogoExportacion(historial: HTMLElement): Promise<void> {
  let pacientes: Paciente[] = [];
  try {
    pacientes = await api.get<Paciente[]>('/pacientes');
  } catch {
    pacientes = [];
  }
  const alcance = el('select', { class: 'control' });
  opciones(alcance, [{ valor: 'COMPLETA', texto: 'Completa (todos los datos)' }, { valor: 'PACIENTE', texto: 'Copia de un paciente' }]);
  const paciente = el('select', { class: 'control' });
  opciones(paciente, pacientes.map((p) => ({ valor: String(p.id), texto: `${p.apellido}, ${p.nombre} (${p.documento})` })), 'Elegí un paciente');
  const campoPaciente = campo('x-paciente', 'Paciente', paciente);
  campoPaciente.hidden = true;
  alcance.addEventListener('change', () => { campoPaciente.hidden = alcance.value !== 'PACIENTE'; });
  const motivo = el('input', { class: 'control', type: 'text', maxlength: 200, placeholder: 'Ej.: pedido del titular (Ley 26.529 art. 14)' });
  abrirDialogo({
    titulo: 'Nueva exportación',
    descripcion: 'La copia por paciente incluye su historia clínica, turnos, presupuestos, señas y notificaciones.',
    campos: [campo('x-alcance', 'Alcance', alcance), campoPaciente, campo('x-motivo', 'Motivo', motivo)],
    accion: 'Exportar',
    validar: () => {
      if (alcance.value === 'PACIENTE' && !paciente.value) return 'Elegí el paciente.';
      return motivo.value.trim() ? null : 'El motivo es obligatorio: queda en la auditoría.';
    },
    enviar: async () => {
      const operacion = await api.post<OperacionCumplimiento>('/cumplimiento/exportaciones', {
        alcance: alcance.value, pacienteId: alcance.value === 'PACIENTE' ? Number(paciente.value) : null, motivo: motivo.value.trim(),
      });
      registrar(operacion, historial);
      toast(operacion.estado === 'COMPLETADA' ? 'Exportación lista para descargar.' : `Exportación ${operacion.estado.toLowerCase()}.`,
        operacion.estado === 'FALLIDA' ? 'error' : 'normal');
    },
  });
}

window.addEventListener('hashchange', () => {
  const pedida = PESTANAS.find((p) => p.id === location.hash.slice(1));
  if (pedida && pedida.id !== actual) { actual = pedida.id; void mostrar(); }
});

await mostrar();
