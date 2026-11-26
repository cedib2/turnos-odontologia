// Diálogo modal con formulario, para las acciones de staff (agenda y cumplimiento).

import { ApiError } from './api.js';
import { aviso, conCarga, el, vaciar } from './ui.js';

export interface OpcionesDialogo {
  titulo: string;
  descripcion?: string;
  campos: HTMLElement[];
  accion: string;
  /** Devuelve un mensaje de validación para cortar el envío, o nada para seguir. */
  validar?: () => string | null;
  enviar: () => Promise<void>;
}

const MOTIVOS_403: Record<string, string> = {
  sin_permiso: 'Tu rol no tiene permiso para esta operación.',
  agenda_de_otro_profesional: 'Solo podés ver tu propia agenda.',
  usuario_sin_profesional_vinculado: 'Tu usuario no está vinculado a un profesional: pedile a administración que lo vincule.',
  ficha_de_paciente_no_atendido: 'Solo podés ver fichas de pacientes que atendés.',
};

/** Mensaje legible de un error de la API de staff. */
export function mensajeStaff(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return error.message;
    if (error.status === 401) return 'Tu sesión venció o no ingresaste. Volvé a ingresar.';
    if (error.status === 403) return MOTIVOS_403[error.codigo ?? ''] ?? 'No tenés permiso para esta operación.';
    // Las validaciones del backend llegan como frase («El destino de la reprogramación está ocupado»).
    if ((error.status === 400 || error.status === 409) && error.codigo && error.codigo.includes(' ')) {
      return error.codigo.endsWith('.') ? error.codigo : `${error.codigo}.`;
    }
    if (error.status === 404) return 'No se encontró el recurso (en este perfil puede no estar disponible).';
    if (error.status === 409 && error.codigo === 'solapamiento') {
      return 'El horario se superpone con otro turno o bloqueo del mismo profesional o sillón. Elegí otro horario o usá un sobreturno.';
    }
    if (error.status === 409 && error.codigo === 'conflicto_de_datos') {
      return 'Ya existe un registro con esos datos (por ejemplo, el mismo documento).';
    }
    if (error.status === 409) return `Conflicto: ${error.codigo ?? 'la operación choca con datos existentes'}.`;
    return `No se pudo completar (${error.status}${error.codigo ? `: ${error.codigo}` : ''}).`;
  }
  return 'Ocurrió un error inesperado.';
}

export function abrirDialogo(opciones: OpcionesDialogo): void {
  const dialogo = el('dialog', { class: 'dialogo', 'aria-labelledby': 'dlg-titulo' });
  const mensajes = el('div', { 'aria-live': 'polite' });
  const enviar = el('button', { class: 'btn', type: 'submit' }, opciones.accion);
  const cerrar = () => { dialogo.close(); dialogo.remove(); };
  const formulario = el('form', { class: 'formulario', novalidate: true },
    el('div', { class: 'dialogo__cuerpo formulario' },
      el('h2', { id: 'dlg-titulo', style: 'margin: 0' }, opciones.titulo),
      opciones.descripcion ? el('p', { class: 'campo__ayuda', style: 'margin: 0' }, opciones.descripcion) : null,
      ...opciones.campos,
      mensajes),
    el('div', { class: 'dialogo__acciones' },
      el('button', { class: 'btn btn--secundario', type: 'button', onclick: cerrar }, 'Cancelar'),
      enviar));

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    vaciar(mensajes);
    const problema = opciones.validar?.();
    if (problema) {
      mensajes.append(aviso('error', problema));
      return;
    }
    void conCarga(enviar, async () => {
      try {
        await opciones.enviar();
        cerrar();
      } catch (error) {
        mensajes.append(aviso('error', mensajeStaff(error)));
      }
    });
  });
  dialogo.addEventListener('cancel', (evento) => { evento.preventDefault(); cerrar(); });
  dialogo.append(formulario);
  document.body.append(dialogo);
  dialogo.showModal();
}
