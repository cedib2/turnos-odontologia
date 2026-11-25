// Copia public/ (HTML y CSS) a src/main/resources/static, donde Spring Boot lo sirve.
import { cpSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const destino = resolve(raiz, '../src/main/resources/static');
mkdirSync(destino, { recursive: true });
cpSync(resolve(raiz, 'public'), destino, { recursive: true });
console.log(`Estáticos copiados a ${destino}`);
