// Cliente HTTP del mismo origen: envía las cookies de sesión de C-04 y, en métodos que modifican
// estado, la cabecera X-CSRF-Token con el valor de la cookie auth_csrf (doble envío).

const COOKIE_CSRF = 'auth_csrf';
const METODOS_SEGUROS = new Set(['GET', 'HEAD', 'OPTIONS']);

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly cuerpo: unknown,
    mensaje: string,
  ) {
    super(mensaje);
    this.name = 'ApiError';
  }

  /** Código de error del backend: `error` en JSON o el texto plano (p. ej. `token-invalido`). */
  get codigo(): string | null {
    if (typeof this.cuerpo === 'string') return this.cuerpo || null;
    if (this.cuerpo && typeof this.cuerpo === 'object' && 'error' in this.cuerpo) {
      const valor = (this.cuerpo as { error: unknown }).error;
      return typeof valor === 'string' ? valor : null;
    }
    return null;
  }
}

export function leerCookie(nombre: string): string | null {
  const prefijo = `${nombre}=`;
  for (const parte of document.cookie.split(';')) {
    const limpia = parte.trim();
    if (limpia.startsWith(prefijo)) return decodeURIComponent(limpia.slice(prefijo.length));
  }
  return null;
}

type Parametros = Record<string, string | number | boolean | null | undefined>;

export function conParametros(ruta: string, parametros: Parametros = {}): string {
  const consulta = new URLSearchParams();
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor !== null && valor !== undefined && valor !== '') consulta.set(clave, String(valor));
  }
  const texto = consulta.toString();
  return texto ? `${ruta}?${texto}` : ruta;
}

// Spring puede devolver un texto plano (p. ej. `credenciales-invalidas`) con Content-Type JSON cuando
// el pedido acepta JSON: se lee como texto y solo se interpreta como JSON si es válido.
async function leerCuerpo(respuesta: Response): Promise<unknown> {
  if (respuesta.status === 204) return null;
  const texto = await respuesta.text();
  if (!texto) return null;
  try {
    return JSON.parse(texto);
  } catch {
    return texto;
  }
}

function mensajeDe(status: number, cuerpo: unknown): string {
  if (typeof cuerpo === 'string' && cuerpo) return cuerpo;
  if (cuerpo && typeof cuerpo === 'object') {
    const datos = cuerpo as Record<string, unknown>;
    for (const clave of ['message', 'error', 'detail']) {
      const valor = datos[clave];
      if (typeof valor === 'string' && valor) return valor;
    }
  }
  return `Error ${status}`;
}

export async function pedir<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<T> {
  const cabeceras: Record<string, string> = { Accept: 'application/json' };
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (!METODOS_SEGUROS.has(metodo.toUpperCase())) {
    const csrf = leerCookie(COOKIE_CSRF);
    if (csrf) cabeceras['X-CSRF-Token'] = csrf;
  }
  let respuesta: Response;
  try {
    respuesta = await fetch(ruta, {
      method: metodo,
      headers: cabeceras,
      credentials: 'same-origin',
      body: cuerpo === undefined ? null : JSON.stringify(cuerpo),
    });
  } catch {
    throw new ApiError(0, null, 'No hay conexión con el servidor.');
  }
  const datos = await leerCuerpo(respuesta);
  if (!respuesta.ok) throw new ApiError(respuesta.status, datos, mensajeDe(respuesta.status, datos));
  return datos as T;
}

export const api = {
  get: <T>(ruta: string, parametros?: Parametros) => pedir<T>('GET', conParametros(ruta, parametros)),
  post: <T>(ruta: string, cuerpo?: unknown) => pedir<T>('POST', ruta, cuerpo ?? {}),
  put: <T>(ruta: string, cuerpo?: unknown) => pedir<T>('PUT', ruta, cuerpo ?? {}),
};

/** Clave de idempotencia para operaciones públicas (C-09). */
export function claveIdempotencia(prefijo: string): string {
  return `${prefijo}-${crypto.randomUUID()}`;
}
