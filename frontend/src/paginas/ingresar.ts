import { api, ApiError, recordarUsuario } from '../api.js';
import { montarLayout } from '../layout.js';
import { $, aviso, campo, conCarga, el, vaciar } from '../ui.js';

montarLayout('ingresar');
const main = $('#contenido');

const MENSAJES: Record<string, string> = {
  'credenciales-invalidas': 'El email o la contraseña no son correctos.',
  'csrf-invalido': 'La sesión del formulario venció. Volvé a intentarlo.',
  'demasiados-intentos': 'Demasiados intentos. Esperá unos minutos antes de volver a probar.',
};

const email = el('input', { class: 'control', type: 'email', name: 'email', autocomplete: 'username', required: true });
const clave = el('input', { class: 'control', type: 'password', name: 'password', autocomplete: 'current-password', required: true });
const mensajes = el('div', { 'aria-live': 'polite' });
const boton = el('button', { class: 'btn btn--bloque', type: 'submit' }, 'Ingresar');
const formulario = el('form', { class: 'formulario', novalidate: true },
  campo('email', 'Email', email), campo('clave', 'Contraseña', clave), mensajes, boton);

vaciar(main, el('div', { style: 'max-width: 26rem; margin-inline: auto' },
  el('div', { class: 'titulo-pagina' },
    el('h1', {}, 'Ingresar'),
    el('p', {}, 'Acceso del personal del consultorio.')),
  el('section', { class: 'tarjeta' }, formulario),
  el('p', { class: 'campo__ayuda', style: 'margin-top: var(--e-4)' },
    'En los perfiles dev y test las pantallas de staff funcionan sin sesión; fuera de ellos exigen ingresar.')));

// El backend exige CSRF de doble envío también en el login: primero se pide la cookie.
const csrfListo = api.get('/auth/csrf').catch(() => undefined);

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  vaciar(mensajes);
  if (!email.value || !clave.value) {
    mensajes.append(aviso('error', 'Completá email y contraseña.'));
    return;
  }
  await conCarga(boton, async () => {
    await csrfListo;
    try {
      await api.post('/auth/login', { email: email.value.trim(), password: clave.value });
      recordarUsuario(email.value.trim());
      const volver = new URLSearchParams(location.search).get('volver');
      location.href = volver && volver.startsWith('/') && !volver.startsWith('//') ? volver : '/agenda.html';
    } catch (error) {
      const codigo = error instanceof ApiError ? error.codigo : null;
      mensajes.append(aviso('error', (codigo && MENSAJES[codigo]) ?? 'No se pudo ingresar. Intentá de nuevo.'));
      clave.value = '';
      clave.focus();
      await api.get('/auth/csrf').catch(() => undefined);
    }
  });
});
