// Mensajes para el paciente según los códigos de error de la reserva pública (C-09).

import { ApiError } from './api.js';
import type { ErrorPublico, Hueco } from './tipos.js';

const MENSAJES: Record<string, string> = {
  token_vencido: 'Este link venció. Pedile al consultorio uno nuevo.',
  jti_reutilizado: 'Este link ya se usó para reservar. Si necesitás otro turno, pedile un link nuevo al consultorio.',
  token_no_valido: 'El link no es válido. Revisá que esté completo o pedí uno nuevo.',
  token_requerido: 'Falta el código del link. Abrí el link completo que te envió el consultorio.',
  hueco_tomado: 'Ese horario se acaba de ocupar. Elegí otro de los disponibles.',
  fuera_de_horario: 'Ese horario está fuera del horario de atención.',
  feriado: 'Ese día es feriado y el consultorio no atiende.',
  fuera_de_ventana: 'Faltan menos de 48 horas hábiles para el turno (o ya empezó): para cambiarlo, comunicate con recepción.',
  turno_inactivo: 'El turno ya no está activo (por ejemplo, porque se canceló).',
  turno_inexistente: 'No encontramos ese turno.',
  turno_ajeno: 'Este link no corresponde a ese turno.',
  paciente_ajeno: 'Este link corresponde a otro paciente.',
  tratamiento_inexistente: 'El tratamiento del link no existe.',
  profesional_inexistente: 'El profesional del link no existe.',
  fecha_invalida: 'La fecha no es válida.',
  operacion_duplicada: 'Esta operación ya se registró.',
};

export interface ProblemaPublico {
  mensaje: string;
  requiereNuevoEnlace: boolean;
  derivarARecepcion: boolean;
  alternativas: Hueco[];
}

export function problemaPublico(error: unknown): ProblemaPublico {
  if (error instanceof ApiError) {
    const cuerpo = (error.cuerpo && typeof error.cuerpo === 'object' ? error.cuerpo : {}) as Partial<ErrorPublico>;
    const codigo = error.codigo ?? '';
    return {
      mensaje: MENSAJES[codigo] ?? (error.status === 0 ? error.message : 'No pudimos completar la operación. Intentá de nuevo.'),
      requiereNuevoEnlace: cuerpo.requiereNuevoEnlace === true || codigo === 'token_vencido',
      derivarARecepcion: cuerpo.derivarARecepcion === true,
      alternativas: cuerpo.alternativas ?? [],
    };
  }
  return { mensaje: 'Ocurrió un error inesperado.', requiereNuevoEnlace: false, derivarARecepcion: false, alternativas: [] };
}
