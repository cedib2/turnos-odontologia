// Fechas en la zona del consultorio (C-01 C2: America/Argentina/Buenos_Aires).
// Argentina no aplica horario de verano desde 2009, así que el offset local es fijo (-03:00).

export const ZONA = 'America/Argentina/Buenos_Aires';
const OFFSET = '-03:00';

// Siempre 24 h: algunos navegadores formatean es-AR en 12 h («09:00 a. m.»), y los <input type="time"> esperan HH:MM.
const fmtHora = new Intl.DateTimeFormat('es-AR', { timeZone: ZONA, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });
const fmtFecha = new Intl.DateTimeFormat('es-AR', { timeZone: ZONA, weekday: 'long', day: 'numeric', month: 'long' });
const fmtFechaCorta = new Intl.DateTimeFormat('es-AR', { timeZone: ZONA, weekday: 'short', day: 'numeric', month: 'short' });
const fmtFechaHora = new Intl.DateTimeFormat('es-AR', {
  timeZone: ZONA, day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
});
const fmtIsoLocal = new Intl.DateTimeFormat('en-CA', { timeZone: ZONA, year: 'numeric', month: '2-digit', day: '2-digit' });

export const hora = (instante: string): string => fmtHora.format(new Date(instante));
export const fecha = (instante: string | Date): string => fmtFecha.format(new Date(instante));
export const fechaCorta = (instante: string | Date): string => fmtFechaCorta.format(new Date(instante));
export const fechaHora = (instante: string | null): string => (instante ? fmtFechaHora.format(new Date(instante)) : '—');

/** Fecha local (AAAA-MM-DD) en la zona del consultorio. */
export const fechaLocal = (instante: string | Date = new Date()): string => fmtIsoLocal.format(new Date(instante));

/** Instante ISO a partir de fecha (AAAA-MM-DD) y hora (HH:MM) locales de Argentina. */
export function instanteLocal(fechaLocalTexto: string, horaLocal: string): string {
  return new Date(`${fechaLocalTexto}T${horaLocal}:00${OFFSET}`).toISOString();
}

/** Suma días a una fecha local AAAA-MM-DD. */
export function sumarDias(fechaLocalTexto: string, dias: number): string {
  const base = new Date(`${fechaLocalTexto}T12:00:00${OFFSET}`);
  base.setUTCDate(base.getUTCDate() + dias);
  return fechaLocal(base);
}

/** Lunes de la semana de la fecha local dada. */
export function lunesDe(fechaLocalTexto: string): string {
  const dia = new Date(`${fechaLocalTexto}T12:00:00${OFFSET}`).getUTCDay();
  return sumarDias(fechaLocalTexto, dia === 0 ? -6 : 1 - dia);
}
