// Agenda del staff (C-08): vista por día y por semana, alta de turnos, bloqueos y sobreturnos, y
// emisión del link de reserva del paciente.

import { api, usuarioRecordado } from '../api.js';
import { fecha, fechaCorta, fechaLocal, hora, instanteLocal, lunesDe, sumarDias } from '../fechas.js';
import { abrirDialogo, mensajeStaff } from '../formularios.js';
import { montarLayout } from '../layout.js';
import type { EnlaceReserva, EntradaAgenda, Paciente, Profesional, Sillon, Tratamiento } from '../tipos.js';
import { $, aviso, campo, confirmacionTurno, el, etiquetaSillon, insignia, opciones, toast, vaciar } from '../ui.js';

montarLayout('agenda');
const main = $('#contenido');

type Vista = 'dia' | 'semana';
const url = new URLSearchParams(location.search);
let vista: Vista = url.get('vista') === 'semana' ? 'semana' : 'dia';
let dia = url.get('fecha') ?? fechaLocal();
let filtroProfesional = url.get('profesional') ?? '';
let filtroSillon = url.get('sillon') ?? '';

let profesionales: Profesional[] = [];
let sillones: Sillon[] = [];
let pacientes: Paciente[] = [];
let tratamientos: Tratamiento[] = [];

const nombreProfesional = (id: number | null) => {
  const p = profesionales.find((x) => x.id === id);
  return p ? `${p.nombre} ${p.apellido}` : id === null ? '—' : `Profesional #${id}`;
};
const nombreSillon = (id: number | null) => {
  const s = sillones.find((x) => x.id === id);
  return s ? etiquetaSillon(s.numero) : id === null ? '—' : `Sillón #${id}`;
};

// Encabezado y barra de herramientas
const titulo = el('h1', { style: 'margin: 0' }, 'Agenda');
const subtitulo = el('p', { class: 'campo__ayuda', style: 'margin: 0' });
const botonesVista = el('div', { class: 'fila', role: 'group', 'aria-label': 'Vista' });
const selectorDia = el('input', { class: 'control', type: 'date', 'aria-label': 'Fecha' });
const selProfesional = el('select', { class: 'control' });
const selSillon = el('select', { class: 'control' });
const resultado = el('div', { 'aria-live': 'polite' });

const acciones = el('div', { class: 'fila' },
  el('button', { class: 'btn', type: 'button', onclick: () => dialogoTurno() }, 'Nuevo turno'),
  el('button', { class: 'btn btn--secundario', type: 'button', onclick: () => dialogoBloqueo() }, 'Bloqueo'),
  el('button', { class: 'btn btn--secundario', type: 'button', onclick: () => dialogoEnlace() }, 'Link de reserva'),
  el('button', { class: 'btn btn--secundario', type: 'button', onclick: () => dialogoPaciente() }, 'Nuevo paciente'));

vaciar(main,
  el('div', { class: 'fila fila--entre titulo-pagina' }, el('div', {}, titulo, subtitulo), acciones),
  el('section', { class: 'tarjeta pila', 'aria-label': 'Filtros' },
    el('div', { class: 'formulario__fila' },
      el('div', { class: 'campo' }, el('span', { class: 'etiqueta' }, 'Vista'), botonesVista),
      el('div', { class: 'campo' }, el('label', { for: 'fecha' }, 'Fecha'),
        el('div', { class: 'fila fila--control' },
          el('button', { class: 'btn btn--secundario', type: 'button', 'aria-label': 'Anterior', onclick: () => mover(-1) }, '←'),
          selectorDia,
          el('button', { class: 'btn btn--secundario', type: 'button', 'aria-label': 'Siguiente', onclick: () => mover(1) }, '→'))),
      campo('f-profesional', 'Profesional', selProfesional),
      campo('f-sillon', 'Sillón', selSillon))),
  el('div', { style: 'margin-top: var(--e-5)' }, resultado));
selectorDia.id = 'fecha';

function pintarBotonesVista(): void {
  vaciar(botonesVista, ...(['dia', 'semana'] as const).map((v) => el('button', {
    class: v === vista ? 'btn btn--chico' : 'btn btn--secundario btn--chico', type: 'button',
    'aria-pressed': String(v === vista), onclick: () => { vista = v; void cargar(); },
  }, v === 'dia' ? 'Día' : 'Semana')));
}

function mover(paso: number): void {
  dia = sumarDias(dia, vista === 'dia' ? paso : paso * 7);
  void cargar();
}

selectorDia.addEventListener('change', () => { dia = selectorDia.value || fechaLocal(); void cargar(); });
selProfesional.addEventListener('change', () => { filtroProfesional = selProfesional.value; void cargar(); });
selSillon.addEventListener('change', () => { filtroSillon = selSillon.value; void cargar(); });

function guardarEnUrl(): void {
  const params = new URLSearchParams({ vista, fecha: dia });
  if (filtroProfesional) params.set('profesional', filtroProfesional);
  if (filtroSillon) params.set('sillon', filtroSillon);
  history.replaceState(null, '', `?${params.toString()}`);
}

async function cargar(): Promise<void> {
  pintarBotonesVista();
  selectorDia.value = dia;
  guardarEnUrl();
  const inicioSemana = lunesDe(dia);
  subtitulo.textContent = vista === 'dia'
    ? fecha(`${dia}T12:00:00-03:00`)
    : `Semana del ${fechaCorta(`${inicioSemana}T12:00:00-03:00`)} al ${fechaCorta(`${sumarDias(inicioSemana, 6)}T12:00:00-03:00`)}`;
  vaciar(resultado, el('p', { class: 'campo__ayuda' }, 'Cargando agenda…'));
  try {
    const filtros = { profesionalId: filtroProfesional || null, sillonId: filtroSillon || null };
    const entradas = vista === 'dia'
      ? await api.get<EntradaAgenda[]>('/agenda/dia', { ...filtros, fecha: dia })
      : await api.get<EntradaAgenda[]>('/agenda/semana', { ...filtros, inicio: inicioSemana });
    entradas.sort((a, b) => a.inicio.localeCompare(b.inicio));
    vaciar(resultado, vista === 'dia' ? vistaDia(entradas) : vistaSemana(entradas, inicioSemana));
  } catch (error) {
    vaciar(resultado, aviso('error', mensajeStaff(error)));
  }
}

function insigniaEstado(entrada: EntradaAgenda): HTMLElement {
  if (entrada.tipo === 'bloqueo') return insignia('Bloqueo');
  const estado = entrada.detalle ?? 'turno';
  const tipo = estado === 'confirmado' ? 'ok' : estado === 'cancelado' ? 'error' : estado === 'reservado' ? 'info' : 'marca';
  return insignia(estado, tipo);
}

function vistaDia(entradas: EntradaAgenda[]): HTMLElement {
  if (!entradas.length) return el('p', { class: 'vacio' }, 'No hay turnos ni bloqueos para este día.');
  return el('ul', { class: 'agenda-dia', style: 'list-style: none; padding: 0; margin: 0' }, ...entradas.map((entrada) =>
    el('li', { class: entrada.tipo === 'bloqueo' ? 'evento evento--bloqueo' : 'evento' },
      el('span', { class: 'evento__hora' }, `${hora(entrada.inicio)}–${hora(entrada.fin)}`),
      el('div', {},
        el('div', { style: 'font-weight: 600' }, entrada.tipo === 'bloqueo'
          ? (entrada.detalle || 'Bloqueo de agenda') : `Turno #${entrada.id}`),
        el('div', { class: 'evento__meta' }, `${nombreProfesional(entrada.profesionalId)} · ${nombreSillon(entrada.sillonId)}`)),
      el('div', { class: 'fila', style: 'justify-content: flex-end; gap: var(--e-2)' },
        insigniaEstado(entrada),
        ...(entrada.tipo === 'turno' && entrada.detalle !== 'cancelado' ? [
          el('button', { class: 'btn btn--fantasma btn--chico', type: 'button', onclick: () => dialogoReprogramar(entrada) }, 'Reprogramar'),
          el('button', { class: 'btn btn--fantasma btn--chico', type: 'button', onclick: () => dialogoSobreturno(entrada) }, 'Sobreturno'),
          el('button', { class: 'btn btn--fantasma btn--chico', type: 'button', onclick: () => dialogoCancelar(entrada) }, 'Cancelar'),
        ] : [])))));
}

function vistaSemana(entradas: EntradaAgenda[], lunes: string): HTMLElement {
  const columnas = Array.from({ length: 7 }, (_, i) => sumarDias(lunes, i)).map((diaSemana) => {
    const delDia = entradas.filter((e) => fechaLocal(e.inicio) === diaSemana);
    return el('section', { class: 'semana__dia', 'aria-label': fecha(`${diaSemana}T12:00:00-03:00`) },
      el('h3', {}, el('button', {
        class: 'btn btn--fantasma btn--chico', type: 'button', style: 'padding: 0',
        onclick: () => { dia = diaSemana; vista = 'dia'; void cargar(); },
      }, fechaCorta(`${diaSemana}T12:00:00-03:00`))),
      delDia.length
        ? el('div', {}, ...delDia.map((e) => el('div', { class: e.tipo === 'bloqueo' ? 'semana__item semana__item--bloqueo' : 'semana__item' },
          `${hora(e.inicio)} ${e.tipo === 'bloqueo' ? 'Bloqueo' : `#${e.id}`} · ${nombreSillon(e.sillonId)}`)))
        : el('p', { class: 'campo__ayuda', style: 'margin: 0' }, 'Libre'));
  });
  return el('div', { class: 'semana' }, ...columnas);
}

// Selectores reutilizables en los diálogos
function selector(items: { valor: string; texto: string }[], vacio?: string, valor = ''): HTMLSelectElement {
  const select = el('select', { class: 'control' });
  opciones(select, items, vacio);
  select.value = valor;
  return select;
}
const itemsProfesionales = () => profesionales.map((p) => ({ valor: String(p.id), texto: `${p.nombre} ${p.apellido}` }));
const itemsSillones = () => sillones.map((s) => ({ valor: String(s.id), texto: etiquetaSillon(s.numero) }));
const itemsPacientes = () => pacientes.map((p) => ({ valor: String(p.id), texto: `${p.apellido}, ${p.nombre} (${p.documento})` }));
const itemsTratamientos = () => tratamientos.map((t) => ({ valor: t.codigo, texto: `${t.nombre} · ${t.duracionMinutos} min` }));

function dialogoTurno(): void {
  const paciente = selector(itemsPacientes(), 'Elegí un paciente');
  const profesional = selector(itemsProfesionales(), 'Elegí un profesional', filtroProfesional);
  const sillon = selector(itemsSillones(), 'Elegí un sillón', filtroSillon);
  const tratamiento = selector(itemsTratamientos(), 'Elegí un tratamiento');
  const diaTurno = el('input', { class: 'control', type: 'date', value: dia });
  const horaTurno = el('input', { class: 'control', type: 'time', step: 900, value: '09:00' });
  const estado = selector([{ valor: 'reservado', texto: 'Reservado' }, { valor: 'confirmado', texto: 'Confirmado' }], undefined, 'reservado');
  abrirDialogo({
    titulo: 'Nuevo turno',
    descripcion: 'La agenda rechaza el turno si choca con otro del mismo profesional o sillón.',
    campos: [
      campo('t-paciente', 'Paciente', paciente),
      el('div', { class: 'formulario__fila' }, campo('t-profesional', 'Profesional', profesional), campo('t-sillon', 'Sillón', sillon)),
      campo('t-tratamiento', 'Tratamiento', tratamiento, 'La duración la define el tratamiento.'),
      el('div', { class: 'formulario__fila' }, campo('t-dia', 'Día', diaTurno), campo('t-hora', 'Hora', horaTurno), campo('t-estado', 'Estado', estado)),
    ],
    accion: 'Crear turno',
    validar: () => (!paciente.value || !profesional.value || !sillon.value || !tratamiento.value || !diaTurno.value || !horaTurno.value
      ? 'Completá todos los campos.' : null),
    enviar: async () => {
      const creado = await api.post<{ inicio: string; fin: string; estado: string }>('/turnos', {
        pacienteId: Number(paciente.value), profesionalId: Number(profesional.value), sillonId: Number(sillon.value),
        codigoTratamiento: tratamiento.value, inicio: instanteLocal(diaTurno.value, horaTurno.value),
        estado: estado.value, origen: 'recepcion',
      });
      dia = diaTurno.value;
      void cargar();
      const datos = [Number(paciente.value), Number(profesional.value), Number(sillon.value)] as const;
      // Se abre después de que el formulario se cierra, para no apilar dos diálogos.
      window.setTimeout(() => mostrarTurnoCreado(creado, ...datos, tratamiento.value), 0);
    },
  });
}

/** Resumen del turno recién creado por el staff, con el aviso que recibe el paciente. */
function mostrarTurnoCreado(turno: { inicio: string; fin: string; estado: string }, pacienteId: number,
                            profesionalId: number, sillonId: number, codigoTratamiento: string): void {
  const p = pacientes.find((x) => x.id === pacienteId);
  const t = tratamientos.find((x) => x.codigo === codigoTratamiento);
  confirmacionTurno({
    motivo: t?.nombre ?? codigoTratamiento,
    estado: turno.estado,
    datos: [
      { etiqueta: 'Paciente', valor: p ? `${p.apellido}, ${p.nombre}` : `Paciente #${pacienteId}` },
      { etiqueta: 'Día', valor: fecha(turno.inicio) },
      { etiqueta: 'Horario', valor: `${hora(turno.inicio)} a ${hora(turno.fin)} hs` },
      { etiqueta: 'Profesional', valor: nombreProfesional(profesionalId) },
      { etiqueta: 'Consultorio', valor: nombreSillon(sillonId) },
    ],
    mensaje: el('p', {}, 'Se programó la confirmación por WhatsApp al paciente y un recordatorio antes del turno. '
      + 'Si se cancela o reprograma, los avisos pendientes se actualizan solos.'),
    acciones: [
      { texto: 'Crear otro turno', primaria: true, alHacer: () => dialogoTurno() },
      { texto: 'Volver a la agenda' },
    ],
  });
}

function dialogoBloqueo(): void {
  const profesional = selector(itemsProfesionales(), 'Elegí un profesional', filtroProfesional);
  const sillon = selector(itemsSillones(), 'Elegí un sillón', filtroSillon);
  const diaBloqueo = el('input', { class: 'control', type: 'date', value: dia });
  const desde = el('input', { class: 'control', type: 'time', step: 900, value: '13:00' });
  const hasta = el('input', { class: 'control', type: 'time', step: 900, value: '14:00' });
  const motivo = el('input', { class: 'control', type: 'text', maxlength: 200, placeholder: 'Ej.: mantenimiento del sillón' });
  abrirDialogo({
    titulo: 'Bloquear agenda',
    campos: [
      el('div', { class: 'formulario__fila' }, campo('b-profesional', 'Profesional', profesional), campo('b-sillon', 'Sillón', sillon)),
      el('div', { class: 'formulario__fila' }, campo('b-dia', 'Día', diaBloqueo), campo('b-desde', 'Desde', desde), campo('b-hasta', 'Hasta', hasta)),
      campo('b-motivo', 'Motivo', motivo),
    ],
    accion: 'Bloquear',
    validar: () => {
      if (!profesional.value || !sillon.value || !diaBloqueo.value || !desde.value || !hasta.value || !motivo.value.trim()) return 'Completá todos los campos.';
      return hasta.value <= desde.value ? 'La hora de fin tiene que ser posterior a la de inicio.' : null;
    },
    enviar: async () => {
      await api.post('/bloqueos', {
        profesionalId: Number(profesional.value), sillonId: Number(sillon.value),
        desde: instanteLocal(diaBloqueo.value, desde.value), hasta: instanteLocal(diaBloqueo.value, hasta.value),
        motivo: motivo.value.trim(),
      });
      toast('Bloqueo creado.');
      dia = diaBloqueo.value;
      void cargar();
    },
  });
}

function dialogoReprogramar(entrada: EntradaAgenda): void {
  const diaNuevo = el('input', { class: 'control', type: 'date', value: fechaLocal(entrada.inicio) });
  const horaNueva = el('input', { class: 'control', type: 'time', step: 900, value: hora(entrada.inicio) });
  abrirDialogo({
    titulo: `Reprogramar el turno #${entrada.id}`,
    descripcion: 'Respeta horarios, feriados y solapamientos. Recepción puede reprogramar aunque falten menos de 48 hs hábiles.',
    campos: [el('div', { class: 'formulario__fila' }, campo('r-dia', 'Nuevo día', diaNuevo), campo('r-hora', 'Nueva hora', horaNueva))],
    accion: 'Reprogramar',
    validar: () => (!diaNuevo.value || !horaNueva.value ? 'Elegí el nuevo día y la hora.' : null),
    enviar: async () => {
      await api.post(`/turnos/${entrada.id}/reprogramacion`, { nuevoInicio: instanteLocal(diaNuevo.value, horaNueva.value) });
      toast('Turno reprogramado.');
      dia = diaNuevo.value;
      void cargar();
    },
  });
}

function dialogoCancelar(entrada: EntradaAgenda): void {
  const motivo = el('textarea', { class: 'control', rows: 3, maxlength: 300, required: true,
    placeholder: 'Ej.: el paciente avisó por teléfono' });
  abrirDialogo({
    titulo: `Cancelar el turno #${entrada.id}`,
    descripcion: `${fecha(entrada.inicio)}, ${hora(entrada.inicio)} hs. El horario queda libre y el motivo se registra en la auditoría.`,
    campos: [campo('c-motivo', 'Motivo', motivo)],
    accion: 'Cancelar turno',
    validar: () => (motivo.value.trim() ? null : 'El motivo es obligatorio.'),
    enviar: async () => {
      await api.post(`/turnos/${entrada.id}/cancelacion`, { motivo: motivo.value.trim() });
      toast('Turno cancelado.');
      void cargar();
    },
  });
}

function dialogoPaciente(): void {
  const tipo = selector([{ valor: 'DNI', texto: 'DNI' }, { valor: 'PASAPORTE', texto: 'Pasaporte' }, { valor: 'CUIT', texto: 'CUIT' }], undefined, 'DNI');
  const documento = el('input', { class: 'control', type: 'text', inputmode: 'numeric', maxlength: 32, autocomplete: 'off' });
  const nombre = el('input', { class: 'control', type: 'text', maxlength: 120, autocomplete: 'off' });
  const apellido = el('input', { class: 'control', type: 'text', maxlength: 120, autocomplete: 'off' });
  const telefono = el('input', { class: 'control', type: 'tel', maxlength: 40, placeholder: '+54 9 11 …' });
  abrirDialogo({
    titulo: 'Nuevo paciente',
    descripcion: 'El teléfono se usa para los avisos por WhatsApp.',
    campos: [
      el('div', { class: 'formulario__fila' }, campo('p-tipo', 'Tipo de documento', tipo), campo('p-documento', 'Número', documento)),
      el('div', { class: 'formulario__fila' }, campo('p-nombre', 'Nombre', nombre), campo('p-apellido', 'Apellido', apellido)),
      campo('p-telefono', 'Teléfono', telefono),
    ],
    accion: 'Dar de alta',
    validar: () => ([documento, nombre, apellido, telefono].some((c) => !c.value.trim()) ? 'Completá todos los campos.' : null),
    enviar: async () => {
      const nuevo = await api.post<Paciente>('/pacientes', {
        documentoTipo: tipo.value, documento: documento.value.trim(), nombre: nombre.value.trim(),
        apellido: apellido.value.trim(), telefono: telefono.value.trim(),
      });
      pacientes = [...pacientes, nuevo].sort((a, b) => a.apellido.localeCompare(b.apellido));
      toast(`Paciente ${nuevo.apellido}, ${nuevo.nombre} dado de alta.`);
    },
  });
}

function dialogoSobreturno(entrada: EntradaAgenda): void {
  const diaNuevo = el('input', { class: 'control', type: 'date', value: fechaLocal(entrada.inicio) });
  const horaNueva = el('input', { class: 'control', type: 'time', step: 300, value: hora(entrada.inicio) });
  const motivo = el('textarea', { class: 'control', maxlength: 300, required: true });
  // Con sesión iniciada, el backend exige que autorice el mismo usuario: se precarga su email.
  const actor = el('input', { class: 'control', type: 'text', maxlength: 120, placeholder: 'Quién autoriza', value: usuarioRecordado() ?? '' });
  abrirDialogo({
    titulo: `Sobreturno del turno #${entrada.id}`,
    descripcion: 'Excepción auditada (C-01 C1): mueve el turno aunque se superponga, con motivo y responsable.',
    campos: [
      el('div', { class: 'formulario__fila' }, campo('s-dia', 'Nuevo día', diaNuevo), campo('s-hora', 'Nueva hora', horaNueva)),
      campo('s-motivo', 'Motivo', motivo),
      campo('s-actor', 'Autoriza', actor),
    ],
    accion: 'Registrar sobreturno',
    validar: () => (!motivo.value.trim() || !actor.value.trim() || !diaNuevo.value || !horaNueva.value ? 'Completá motivo, responsable, día y hora.' : null),
    enviar: async () => {
      await api.post('/sobreturnos', {
        turnoId: entrada.id, nuevoInicio: instanteLocal(diaNuevo.value, horaNueva.value),
        motivo: motivo.value.trim(), actor: actor.value.trim(),
      });
      toast('Sobreturno registrado.');
      void cargar();
    },
  });
}

function dialogoEnlace(): void {
  const paciente = selector(itemsPacientes(), 'Elegí un paciente');
  const profesional = selector(itemsProfesionales(), 'Elegí un profesional', filtroProfesional);
  const tratamiento = selector(itemsTratamientos(), 'Elegí un tratamiento');
  const sillon = selector(itemsSillones(), 'Elegí un sillón', filtroSillon);
  abrirDialogo({
    titulo: 'Link de reserva',
    descripcion: 'Genera un link válido por 1 hora para que el paciente elija su horario. Se usa una sola vez.',
    campos: [
      campo('e-paciente', 'Paciente', paciente),
      el('div', { class: 'formulario__fila' }, campo('e-profesional', 'Profesional', profesional), campo('e-sillon', 'Sillón', sillon)),
      campo('e-tratamiento', 'Tratamiento', tratamiento),
    ],
    accion: 'Generar link',
    validar: () => (!paciente.value || !profesional.value || !sillon.value || !tratamiento.value
      ? 'Elegí paciente, profesional, sillón y tratamiento.' : null),
    enviar: async () => {
      const enlace = await api.post<EnlaceReserva>(`/pacientes/${paciente.value}/enlace-reserva`, {
        profesionalId: Number(profesional.value), codigoTratamiento: tratamiento.value, sillonId: Number(sillon.value),
      });
      mostrarEnlace(new URL(enlace.ruta, location.origin).toString(), enlace.venceAt);
    },
  });
}

function mostrarEnlace(direccion: string, venceAt: string): void {
  const salida = el('input', { class: 'control mono', type: 'text', readonly: true, value: direccion });
  const copiar = el('button', { class: 'btn btn--secundario', type: 'button' }, 'Copiar');
  copiar.addEventListener('click', async () => {
    await navigator.clipboard?.writeText(direccion).catch(() => undefined);
    copiar.textContent = 'Copiado';
  });
  abrirDialogo({
    titulo: 'Link generado',
    descripcion: `Vence a las ${hora(venceAt)}. Envialo al paciente por WhatsApp.`,
    campos: [campo('e-link', 'Link', salida), el('div', { class: 'fila' }, copiar,
      el('a', { class: 'btn btn--fantasma', href: direccion, target: '_blank', rel: 'noopener' }, 'Abrir'))],
    accion: 'Listo',
    enviar: async () => undefined,
  });
}

async function cargarDirectorio(): Promise<void> {
  const [ps, ss, pacs, trs] = await Promise.allSettled([
    api.get<Profesional[]>('/profesionales'), api.get<Sillon[]>('/sillones'),
    api.get<Paciente[]>('/pacientes'), api.get<Tratamiento[]>('/tratamientos'),
  ]);
  profesionales = ps.status === 'fulfilled' ? ps.value : [];
  sillones = ss.status === 'fulfilled' ? ss.value : [];
  pacientes = pacs.status === 'fulfilled' ? pacs.value : [];
  tratamientos = trs.status === 'fulfilled' ? trs.value.filter((t) => t.activo !== false) : [];
  opciones(selProfesional, itemsProfesionales(), 'Todos');
  opciones(selSillon, itemsSillones(), 'Todos');
  selProfesional.value = filtroProfesional;
  selSillon.value = filtroSillon;
  if ([ps, ss, pacs, trs].some((r) => r.status === 'rejected')) {
    main.insertBefore(aviso('alerta', 'No se pudieron cargar todos los listados (profesionales, sillones, pacientes o tratamientos).'), main.children[1] ?? null);
  }
}

await cargarDirectorio();
await cargar();
