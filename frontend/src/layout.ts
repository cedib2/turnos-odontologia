// Encabezado, navegación responsiva y pie comunes a las páginas de staff.

import { api, ApiError, olvidarUsuario, prepararCsrf, usuarioRecordado } from './api.js';
import type { Sesion } from './tipos.js';
import { el, vaciar } from './ui.js';

export type Seccion = 'inicio' | 'agenda' | 'fichas' | 'cumplimiento' | 'ingresar' | 'paciente';

const ENLACES: { seccion: Seccion; href: string; texto: string }[] = [
  { seccion: 'inicio', href: '/', texto: 'Inicio' },
  { seccion: 'agenda', href: '/agenda.html', texto: 'Agenda' },
  { seccion: 'fichas', href: '/fichas.html', texto: 'Fichas' },
  { seccion: 'cumplimiento', href: '/cumplimiento.html', texto: 'Cumplimiento' },
];

function marca(): HTMLElement {
  return el('a', { class: 'marca', href: '/' },
    el('span', { class: 'marca__icono', 'aria-hidden': 'true' }, '✚'),
    el('span', {}, 'Turnos Odontología'));
}

/** Monta el encabezado. En las páginas del paciente no muestra la navegación de staff. */
export function montarLayout(actual: Seccion): void {
  const encabezado = el('header', { class: 'encabezado' });
  const barra = el('div', { class: 'contenedor encabezado__barra' }, marca());

  if (actual !== 'paciente') {
    const nav = el('nav', { class: 'nav', id: 'nav-principal', 'aria-label': 'Principal' });
    const lista = el('ul', { class: 'nav__lista' });
    for (const enlace of ENLACES) {
      lista.append(el('li', {}, el('a', {
        class: 'nav__enlace', href: enlace.href,
        'aria-current': enlace.seccion === actual ? 'page' : null,
      }, enlace.texto)));
    }
    nav.append(lista);
    const boton = el('button', {
      class: 'btn btn--fantasma nav__boton', type: 'button',
      'aria-controls': 'nav-principal', 'aria-expanded': 'false',
    }, 'Menú');
    boton.addEventListener('click', () => {
      const abierta = nav.getAttribute('data-abierta') === 'true';
      nav.setAttribute('data-abierta', String(!abierta));
      boton.setAttribute('aria-expanded', String(!abierta));
    });
    const sesion = el('div', { class: 'sesion', id: 'sesion' });
    barra.append(boton, nav, sesion);
    void pintarSesion(sesion);
  }

  encabezado.append(barra);
  document.body.prepend(el('a', { class: 'saltar', href: '#contenido' }, 'Saltar al contenido'), encabezado);
  document.body.append(el('footer', { class: 'pie' },
    el('div', { class: 'contenedor' }, 'Horarios en hora de Argentina. Datos tratados según la Ley 25.326.')));
}

function pintarSesionActiva(nodo: HTMLElement, texto: string): void {
  nodo.replaceChildren(
    el('span', {}, texto),
    el('button', {
      class: 'btn btn--fantasma btn--chico', type: 'button',
      onclick: async () => {
        // La sesión solo se cierra acá. Se pide un CSRF vigente por si el servidor se reinició.
        await prepararCsrf();
        await api.post('/auth/logout').catch(() => undefined);
        olvidarUsuario();
        window.location.href = '/ingresar.html';
      },
    }, 'Salir'));
}

async function pintarSesion(nodo: HTMLElement): Promise<void> {
  try {
    const sesion = await api.get<Sesion>('/auth/me');
    const usuario = usuarioRecordado();
    pintarSesionActiva(nodo, usuario ? `Sesión: ${usuario} (${sesion.rol})` : `Sesión: ${sesion.rol}`);
  } catch (error) {
    // 403: el access es válido pero el rol (p. ej. recepcionista o auditor) no tiene el permiso de
    // ámbito «propio» que C-04 exige en /auth/me. La sesión está iniciada igual.
    if (error instanceof ApiError && error.status === 403) {
      pintarSesionActiva(nodo, `Sesión: ${usuarioRecordado() ?? 'iniciada'}`);
      return;
    }
    const sinSesion = error instanceof ApiError && error.status === 401;
    vaciar(nodo,
      sinSesion ? el('span', { title: 'En dev/test la superficie de staff no exige sesión' }, 'Sin sesión') : null,
      el('a', { class: 'btn btn--secundario btn--chico', href: '/ingresar.html' }, 'Ingresar'));
  }
}
