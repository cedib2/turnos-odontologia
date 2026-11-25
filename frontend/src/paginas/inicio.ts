import { api } from '../api.js';
import { montarLayout } from '../layout.js';
import type { PrecioPublico } from '../tipos.js';
import { $, aviso, el, insignia, vaciar } from '../ui.js';

montarLayout('inicio');
const main = $('#contenido');

const tarjetaPrecio = el('section', { class: 'tarjeta', 'aria-labelledby': 'precio-titulo' },
  el('h2', { id: 'precio-titulo' }, 'Precio'), el('p', {}, 'Cargando…'));

function acceso(href: string, icono: string, titulo: string, texto: string): HTMLElement {
  return el('a', { class: 'tarjeta acceso', href },
    el('span', { class: 'acceso__icono', 'aria-hidden': 'true' }, icono),
    el('h2', {}, titulo),
    el('p', { class: 'campo__ayuda' }, texto));
}

vaciar(main,
  el('section', { class: 'hero' },
    el('h1', {}, 'Turnos sin llamadas, agenda sin solapamientos'),
    el('p', {}, 'Los pacientes reservan desde un link o un QR, el consultorio ve su agenda al día y el cumplimiento queda documentado con evidencia.')),
  el('div', { class: 'rejilla' },
    acceso('/agenda.html', '📅', 'Agenda', 'Turnos, bloqueos y sobreturnos por día y por semana. Generá links de reserva.'),
    acceso('/cumplimiento.html', '🛡️', 'Cumplimiento', 'Métricas con metodología, dossier, exportaciones y respaldos.'),
    acceso('/ingresar.html', '🔑', 'Ingresar', 'Acceso del personal con la sesión del consultorio.')),
  el('div', { class: 'rejilla--2 rejilla', style: 'margin-top: var(--e-5)' },
    el('section', { class: 'tarjeta', 'aria-labelledby': 'paciente-titulo' },
      el('h2', { id: 'paciente-titulo' }, '¿Sos paciente?'),
      el('p', {}, 'Abrí el link de reserva que te envió el consultorio por WhatsApp o escaneá el QR. Desde ese mismo link vas a poder confirmar, reprogramar o cancelar tu turno.')),
    tarjetaPrecio));

try {
  const precio = await api.get<PrecioPublico>('/precio');
  const pendiente = precio.monto === null;
  vaciar(tarjetaPrecio,
    el('div', { class: 'tarjeta__titulo' },
      el('h2', { id: 'precio-titulo' }, precio.plan ?? 'Plan único'),
      insignia(pendiente ? 'Pendiente de aprobación' : 'Vigente', pendiente ? 'alerta' : 'ok')),
    el('p', { class: 'precio' }, pendiente
      ? 'A definir'
      : `${precio.moneda ?? ''} ${precio.monto?.toLocaleString('es-AR')} / ${precio.periodicidad ?? ''}`),
    precio.todoIncluido ? el('p', {}, 'Todo incluido: sin costos de API separados ni packs adicionales.') : null,
    precio.incluye.length ? el('ul', {}, ...precio.incluye.map((item) => el('li', {}, item))) : null,
    precio.nota ? el('p', { class: 'campo__ayuda' }, precio.nota) : null);
} catch {
  vaciar(tarjetaPrecio, el('h2', { id: 'precio-titulo' }, 'Precio'),
    aviso('info', 'El precio todavía no está publicado.'));
}
