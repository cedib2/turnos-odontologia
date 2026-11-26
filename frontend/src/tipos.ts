// Contratos de la API del backend (Spring Boot). Los instantes llegan como texto ISO-8601 en UTC.

export type Instante = string;

// Reserva pública (C-06/C-09)
export interface Hueco {
  inicio: Instante;
  fin: Instante;
}

export interface Disponibilidad {
  profesionalId: number;
  sillonId: number | null;
  fecha: string;
  codigoTratamiento: string;
  huecos: Hueco[];
}

export interface CrearTurnoPublico {
  pacienteId: number;
  profesionalId: number;
  sillonId: number;
  codigoTratamiento: string;
  inicio: Instante;
  idempotency_key: string;
}

export interface TurnoPublicoCreado {
  turnoId: number;
  inicio: Instante;
  fin: Instante;
  estado: string;
  origen: string;
  tokenGestionar: string;
  /** Nombres para mostrar en la confirmación (pueden faltar). */
  tratamiento?: string | null;
  profesional?: string | null;
  sillon?: string | null;
  consultorio?: string | null;
}

export interface TurnoGestionado {
  turnoId: number;
  estado: string;
  activo: boolean;
  inicio: Instante;
  fin: Instante;
}

export interface ErrorPublico {
  error: string;
  requiereNuevoEnlace: boolean | null;
  derivarARecepcion: boolean | null;
  alternativas: Hueco[] | null;
}

// Directorio y agenda (dev/test)
export interface Paciente {
  id: number;
  documento: string;
  nombre: string;
  apellido: string;
  telefono: string | null;
}

export interface Profesional {
  id: number;
  matricula: string;
  nombre: string;
  apellido: string;
  especialidad: string | null;
}

export interface Sillon {
  id: number;
  numero: string;
  consultorioId: number;
}

export interface Tratamiento {
  id: number;
  codigo: string;
  nombre: string;
  duracionMinutos: number;
  activo?: boolean;
}

export interface EnlaceReserva {
  token: string;
  ruta: string;
  venceAt: Instante;
}

export interface EntradaAgenda {
  tipo: 'turno' | 'bloqueo' | string;
  id: number;
  inicio: Instante;
  fin: Instante;
  profesionalId: number | null;
  sillonId: number | null;
  detalle: string | null;
}

export interface TurnoVista {
  id: number;
  inicio: Instante;
  fin: Instante;
  estado: string;
  activo: boolean;
  profesionalId: number;
  sillonId: number;
}

// Ficha clínica (C-11/C-12)
export interface RegistroOdontograma {
  id: number;
  piezaFdi: number;
  temporal: boolean;
  cara: string | null;
  estado: string;
  version: number;
  fecha: Instante;
  autor: string;
}

export interface HistoriaClinica {
  fichaId: number;
  pacienteId: number;
  historiaVersion: number;
  anamnesis: {
    id: number;
    version: number;
    respuestas: Record<string, unknown>;
    textoLibre: string | null;
    fecha: Instante;
    autor: string;
  } | null;
  odontograma: RegistroOdontograma[];
  evoluciones: { id: number; profesionalId: number; version: number; reemplazaId: number | null; texto: string; fecha: Instante; autor: string }[];
  consentimientos: { id: number; tipo: string; version: number; texto: string; fecha: Instante; autor: string }[];
}

/** Historia del titular vista con su link (C-13). */
export interface HistoriaDelTitular {
  tieneFicha: boolean;
  historia: HistoriaClinica | null;
}

// Cumplimiento (C-17/C-18) y métricas (C-22)
export interface Metrica {
  codigo: string;
  definicion: string;
  formula: string;
  fuente: string;
  periodoDesde: Instante;
  periodoHasta: Instante;
  zonaHoraria: string;
  estado: 'CALCULADA' | 'SIN_DATOS' | 'NO_DISPONIBLE' | 'DIFERIDA' | string;
  numerador: number | null;
  denominador: number | null;
  valor: number | null;
  meta: string;
  detalle: Record<string, unknown>;
}

export interface ItemDossier {
  codigo: string;
  descripcion: string;
  categoria: string;
  fuente: 'MANUAL' | 'AUTOMATICA' | string;
  baseNormativa: string;
  obligatorio: boolean;
  estado: 'PRESENTE' | 'PENDIENTE' | string;
  evidencia: string | null;
  verificadoPor: string | null;
  verificadoAt: Instante | null;
}

export interface Subprocesador {
  id: number;
  nombre: string;
  servicio: string;
  ubicacionDatos: string | null;
  clausulasAaipReferencia: string | null;
  dpaReferencia: string | null;
}

export interface Dossier {
  generadoAt: Instante;
  items: ItemDossier[];
  subprocesadoresIncluidos: Subprocesador[];
  subprocesadoresExcluidos: Subprocesador[];
  pendientes: string[];
  completitud: {
    presentes: number;
    exigidos: number;
    porcentaje: number | null;
    formula: string;
    metodologia: string;
    meta: string;
  };
  matrizRoles: Record<string, string[]>;
  parametrosOperativos: Record<string, string>;
  publicable: boolean;
}

export interface OperacionCumplimiento {
  id: number;
  tipo: string;
  alcance: string;
  pacienteId: number | null;
  estado: string;
  formato: string | null;
  destino: string | null;
  cifrado: boolean;
  respaldoOrigenId: number | null;
  entornoRestore: string | null;
  motivo: string | null;
  solicitadoPor: string | null;
  solicitadoAt: Instante;
  plazoVenceAt: Instante | null;
  completadoAt: Instante | null;
  retenerHasta: Instante | null;
  checksumSha256: string | null;
  tablas: number | null;
  filas: number | null;
  error: string | null;
}

export interface PrecioPublico {
  modelo: string;
  estado: string;
  plan: string | null;
  monto: number | null;
  moneda: string | null;
  periodicidad: string | null;
  incluye: string[];
  todoIncluido: boolean;
  desgloseCostosApi: boolean;
  packsAdicionales: boolean;
  vigenteDesde: string | null;
  nota: string | null;
}

// Sesión de staff (C-04)
export interface Sesion {
  usuario_id: number;
  rol: string;
}
