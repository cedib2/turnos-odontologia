// Reserva pública por link/QR (C-09). El link trae el token RESERVAR, el paciente, el profesional,
// el sillón y el tratamiento: el backend exige el sillón al crear el turno.

import { api, claveIdempotencia } from '../api.js';
import { fecha, fechaLocal, hora, sumarDias } from '../fechas.js';
import { montarLayout } from '../layout.js';
import { problemaPublico } from '../publico.js';
import type { Disponibilidad, Hueco, TurnoPublicoCreado } from '../tipos.js';
import { $, aviso, conCarga, el, vaciar } from '../ui.js';

montarLayout('paciente');
const main = $('#contenido');

const parametros = new URLSearchParams(location.search);
const token = parametros.get('token');
const pacienteId = Number(parametros.get('paciente'));
const profesionalId = Number(parametros.get('profesional'));
const tratamiento = parametros.get('tratamiento');
const sillonId = Number(parametros.get('sillon'));

const hoy = fechaLocal();
let dia = hoy;
let elegido: Hueco | null = null;
// Clave estable mientras el paciente no cambie de horario: un reintento no crea dos turnos.
let clave = claveIdempotencia('reserva');

const pasos = el('ol', { class: 'pasos', 'aria-label': 'Pasos de la reserva' });
const contenido = el('div', { class: 'pila' });

function marcarPaso(actual: 1 | 2 | 3): void {
  vaciar(pasos, ...['Elegí horario', 'Confirmá', 'Listo'].map((texto, i) =>
    el('li', { class: i + 1 < actual ? 'hecho' : '', 'aria-current': i + 1 === actual ? 'step' : null },
      el('span', {}, texto))));
}

vaciar(main,
  el('div', { style: 'max-width: 40rem; margin-inline: auto' },
    el('div', { class: 'titulo-pagina' },
      el('h1', {}, 'Reservá tu turno'),
      el('p', {}, 'Elegí el día y el horario que te quede mejor. Los horarios son de Argentina.')),
    pasos, contenido));

if (!token || !pacienteId || !profesionalId || !sillonId || !tratamiento) {
  marcarPaso(1);
  vaciar(contenido, aviso('error',
    el('p', {}, el('strong', {}, 'El link está incompleto.')),
    el('p', {}, 'Abrí el link completo que te envió el consultorio por WhatsApp, o pedí uno nuevo.')));
} else {
  void pasoHorario();
}

async function pasoHorario(alternativas?: Hueco[], mensaje?: string): Promise<void> {
  marcarPaso(1);
  elegido = null;
  const selectorDia = el('input', { class: 'control', type: 'date', id: 'dia', value: dia, min: hoy });
  const anterior = el('button', { class: 'btn btn--secundario', type: 'button', 'aria-label': 'Día anterior' }, '←');
  const siguiente = el('button', { class: 'btn btn--secundario', type: 'button', 'aria-label': 'Día siguiente' }, '→');
  anterior.disabled = dia <= hoy;
  const lista = el('div', { 'aria-live': 'polite' });
  const continuar = el('button', { class: 'btn btn--bloque', type: 'button', disabled: true }, 'Continuar');

  const cambiarDia = (nuevo: string) => { dia = nuevo < hoy ? hoy : nuevo; void pasoHorario(); };
  selectorDia.addEventListener('change', () => cambiarDia(selectorDia.value || hoy));
  anterior.addEventListener('click', () => cambiarDia(sumarDias(dia, -1)));
  siguiente.addEventListener('click', () => cambiarDia(sumarDias(dia, 1)));
  continuar.addEventListener('click', () => { if (elegido) pasoConfirmar(elegido); });

  vaciar(contenido,
    mensaje ? aviso('alerta', mensaje) : null,
    el('section', { class: 'tarjeta pila' },
      el('div', { class: 'campo' },
        el('label', { for: 'dia' }, 'Día'),
        el('div', { class: 'fila fila--control' }, anterior, selectorDia, siguiente)),
      el('h2', { style: 'font-size: var(--t-md); margin: 0' }, fecha(`${dia}T12:00:00-03:00`)),
      lista),
    continuar);

  const pintar = (huecos: Hueco[]) => {
    if (!huecos.length) {
      vaciar(lista, el('p', { class: 'vacio' }, 'No hay horarios libres este día. Probá con otro.'));
      return;
    }
    const grupo = el('div', { class: 'huecos', role: 'group', 'aria-label': 'Horarios disponibles' });
    for (const hueco of huecos) {
      const boton = el('button', { class: 'hueco', type: 'button', 'aria-pressed': 'false' }, hora(hueco.inicio));
      boton.addEventListener('click', () => {
        grupo.querySelectorAll('.hueco').forEach((b) => b.setAttribute('aria-pressed', 'false'));
        boton.setAttribute('aria-pressed', 'true');
        elegido = hueco;
        continuar.disabled = false;
      });
      grupo.append(boton);
    }
    vaciar(lista, grupo);
  };

  if (alternativas?.length) {
    pintar(alternativas);
    return;
  }
  vaciar(lista, el('p', { class: 'campo__ayuda' }, 'Buscando horarios…'));
  try {
    const disponibilidad = await api.get<Disponibilidad>('/publico/v1/turnos/disponibilidad', {
      token, profesionalId, sillonId, fecha: dia, codigoTratamiento: tratamiento,
    });
    // El backend lista también los horarios de hoy que ya pasaron: no se ofrecen.
    const ahora = Date.now();
    pintar(disponibilidad.huecos.filter((hueco) => new Date(hueco.inicio).getTime() > ahora));
  } catch (error) {
    const problema = problemaPublico(error);
    vaciar(lista, aviso(problema.requiereNuevoEnlace ? 'error' : 'alerta', problema.mensaje));
  }
}

function pasoConfirmar(hueco: Hueco): void {
  marcarPaso(2);
  const mensajes = el('div', { 'aria-live': 'polite' });
  const confirmar = el('button', { class: 'btn btn--bloque', type: 'button' }, 'Confirmar reserva');
  const volver = el('button', { class: 'btn btn--secundario btn--bloque', type: 'button' }, 'Elegir otro horario');
  volver.addEventListener('click', () => { clave = claveIdempotencia('reserva'); void pasoHorario(); });

  confirmar.addEventListener('click', () => conCarga(confirmar, async () => {
    vaciar(mensajes);
    try {
      const creado = await api.post<TurnoPublicoCreado>(`/publico/v1/turnos?token=${encodeURIComponent(token ?? '')}`, {
        pacienteId, profesionalId, sillonId, codigoTratamiento: tratamiento, inicio: hueco.inicio, idempotency_key: clave,
      });
      pasoListo(creado);
    } catch (error) {
      const problema = problemaPublico(error);
      if (problema.alternativas.length) {
        clave = claveIdempotencia('reserva');
        void pasoHorario(problema.alternativas, `${problema.mensaje} Estos son los horarios más cercanos:`);
        return;
      }
      mensajes.append(aviso('error', problema.mensaje));
    }
  }));

  vaciar(contenido,
    el('section', { class: 'tarjeta pila' },
      el('h2', {}, 'Revisá tu turno'),
      el('dl', { style: 'margin: 0; display: grid; gap: var(--e-2)' },
        el('dt', { class: 'campo__ayuda' }, 'Día'), el('dd', { style: 'margin: 0; font-weight: 600' }, fecha(hueco.inicio)),
        el('dt', { class: 'campo__ayuda' }, 'Horario'), el('dd', { style: 'margin: 0; font-weight: 600' }, `${hora(hueco.inicio)} a ${hora(hueco.fin)}`)),
      el('p', { class: 'campo__ayuda' }, 'Los cambios por link están sujetos a la ventana de 48 horas hábiles del consultorio; si no se pueden hacer, te vamos a indicar que contactes a recepción.'),
      mensajes),
    confirmar, volver);
  confirmar.focus();
}

function pasoListo(turno: TurnoPublicoCreado): void {
  marcarPaso(3);
  const gestion = new URL('/turno.html', location.origin);
  gestion.searchParams.set('token', turno.tokenGestionar);
  gestion.searchParams.set('turno', String(turno.turnoId));
  gestion.searchParams.set('inicio', turno.inicio);
  gestion.searchParams.set('fin', turno.fin);
  const copiar = el('button', { class: 'btn btn--secundario btn--bloque', type: 'button' }, 'Copiar link de mi turno');
  copiar.addEventListener('click', async () => {
    await navigator.clipboard?.writeText(gestion.toString()).catch(() => undefined);
    copiar.textContent = 'Link copiado';
  });

  vaciar(contenido,
    aviso('ok', el('p', {}, el('strong', {}, '¡Listo! Tu turno quedó reservado.'))),
    el('section', { class: 'tarjeta pila' },
      el('p', { class: 'metrica__valor', style: 'font-size: var(--t-xl)' }, `${hora(turno.inicio)} hs`),
      el('p', {}, fecha(turno.inicio)),
      el('p', { class: 'campo__ayuda' }, 'Guardá el link de tu turno: con él podés confirmar asistencia, reprogramar o cancelar.')),
    el('a', { class: 'btn btn--bloque', href: gestion.pathname + gestion.search }, 'Ver mi turno'),
    copiar);
}
