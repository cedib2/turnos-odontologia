// Borra la salida anterior para que no queden archivos de páginas eliminadas.
import { rmSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..');
rmSync(resolve(raiz, '../src/main/resources/static'), { recursive: true, force: true });
