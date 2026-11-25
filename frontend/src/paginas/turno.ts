// Gestión del turno por el paciente con el token GESTIONAR (C-09): confirmar, reprogramar o cancelar.

import { api, claveIdempotencia } from '../api.js';
import { fecha, fechaLocal, hora, instanteLocal } from '../fechas.js';
import { montarLayout } from '../layout.js';
import { problemaPublico } from '../publico.js';
import type { Hueco, TurnoGestionado } from '../tipos.js';
import { $, aviso, campo, confirmar, conCarga, el, insignia, vaciar } from '../ui.js';

montarLayout('paciente');
const main = $('#contenido');

const parametros = new URLSearchParams(location.search);
const token = parametros.get('token') ?? '';
const turnoId = Number(parametros.get('turno'));
const estadoInicial: Partial<TurnoGestionado> = {
  turnoId,
  inicio: parametros.get('inicio') ?? '',
  fin: parametros.get('fin') ?? '',
};

const resumen = el('section', { class: 'tarjeta pila', 'aria-live': 'polite' });
const mensajes = el('div', { 'aria-live': 'polite' });
const acciones = el('div', { class: 'pila' });

vaciar(main, el('div', { style: 'max-width: 40rem; margin-inline: auto' },
  el('div', { class: 'titulo-pagina' }, el('h1', {}, 'Tu turno')),
  el('div', { class: 'pila' }, resumen, mensajes, acciones)));

const ruta = (accion: string) => `/publico/v1/turnos/${accion}?token=${encodeURIComponent(token)}`;

function pintarResumen(turno: Partial<TurnoGestionado>): void {
  const estado = turno.estado;
  const tipo = estado === 'cancelado' ? 'error' : estado === 'confirmado' ? 'ok' : 'info';
  vaciar(resumen,
    el('div', { class: 'tarjeta__titulo' },
      el('h2', {}, `Turno #${turno.turnoId}`),
      estado ? insignia(estado, tipo) : null),
    turno.inicio
      ? el('div', {}, el('p', { class: 'metrica__valor', style: 'font-size: var(--t-xl)' }, `${hora(turno.inicio)} hs`),
        el('p', { style: 'margin: 0' }, fecha(turno.inicio)))
      : el('p', { class: 'campo__ayuda' }, 'Usá las opciones de abajo para gestionar tu turno.'));
}

function mostrarProblema(error: unknown, alAlternativa?: (hueco: Hueco) => void): void {
  const problema = problemaPublico(error);
  const lista = problema.alternativas.length && alAlternativa
    ? el('div', { class: 'huecos', style: 'margin-top: var(--e-3)' },
      ...problema.alternativas.map((hueco) => el('button', {
        class: 'hueco', type: 'button', onclick: () => alAlternativa(hueco),
      }, `${hora(hueco.inicio)}`)))
    : null;
  vaciar(mensajes, aviso(problema.derivarARecepcion ? 'alerta' : 'error', el('p', {}, problema.mensaje), lista));
}

async function gestionar(accion: 'confirmar' | 'cancelar', boton: HTMLButtonElement): Promise<void> {
  await conCarga(boton, async () => {
    vaciar(mensajes);
    try {
      const turno = await api.post<TurnoGestionado>(ruta(accion), { turnoId, idempotency_key: claveIdempotencia(accion) });
      pintarResumen(turno);
      mensajes.append(aviso('ok', accion === 'confirmar' ? 'Confirmaste tu asistencia. ¡Te esperamos!' : 'Tu turno quedó cancelado.'));
      if (!turno.activo) vaciar(acciones);
    } catch (error) {
      mostrarProblema(error);
    }
  });
}

function pintarAcciones(): void {
  const confirmarBtn = el('button', { class: 'btn btn--bloque', type: 'button' }, 'Confirmar asistencia');
  confirmarBtn.addEventListener('click', () => void gestionar('confirmar', confirmarBtn));

  const cancelarBtn = el('button', { class: 'btn btn--peligro btn--bloque', type: 'button' }, 'Cancelar turno');
  cancelarBtn.addEventListener('click', async () => {
    if (await confirmar('¿Cancelar el turno?', 'El horario quedará libre para otro paciente.', 'Sí, cancelar', true)) {
      await gestionar('cancelar', cancelarBtn);
    }
  });

  const dia = el('input', { class: 'control', type: 'date', min: fechaLocal(), required: true });
  const horario = el('input', { class: 'control', type: 'time', step: 900, required: true });
  const reprogramar = el('button', { class: 'btn btn--secundario', type: 'submit' }, 'Reprogramar');
  const formulario = el('form', { class: 'formulario' },
    el('div', { class: 'formulario__fila' }, campo('nuevo-dia', 'Nuevo día', dia), campo('nuevo-horario', 'Nuevo horario', horario)),
    reprogramar);

  const enviar = (inicio: string) => conCarga(reprogramar, async () => {
    vaciar(mensajes);
    try {
      const turno = await api.post<TurnoGestionado>(ruta('reprogramar'), {
        turnoId, inicio, idempotency_key: claveIdempotencia('reprogramar'),
      });
      pintarResumen(turno);
      mensajes.append(aviso('ok', 'Listo, tu turno quedó reprogramado.'));
    } catch (error) {
      mostrarProblema(error, (hueco) => void enviar(hueco.inicio));
    }
  });
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (!dia.value || !horario.value) {
      vaciar(mensajes, aviso('error', 'Elegí el nuevo día y horario.'));
      return;
    }
    void enviar(instanteLocal(dia.value, horario.value));
  });

  vaciar(acciones,
    confirmarBtn,
    el('details', { class: 'tarjeta tarjeta--plana' },
      el('summary', { style: 'cursor: pointer; font-weight: 600' }, 'Reprogramar'),
      el('p', { class: 'campo__ayuda' }, 'Si el horario no está libre, te mostramos los más cercanos.'),
      formulario),
    cancelarBtn,
    el('p', { class: 'campo__ayuda' }, 'Podés confirmar tu asistencia hasta el inicio del turno. Cancelar o reprogramar por link se puede hasta 48 horas hábiles antes; después, comunicate con recepción.'));
}

if (!token || !turnoId) {
  vaciar(resumen, aviso('error', 'El link del turno está incompleto. Abrí el link completo que recibiste al reservar.'));
} else {
  pintarResumen(estadoInicial);
  pintarAcciones();
}
