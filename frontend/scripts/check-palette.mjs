import { readdir, readFile } from 'node:fs/promises'
import { join } from 'node:path'
const walk = async (dir) => (await readdir(dir, { withFileTypes: true })).flatMap(async (entry) => entry.isDirectory() ? walk(join(dir, entry.name)) : [join(dir, entry.name)])
const files = (await Promise.all(await walk(new URL('../apps', import.meta.url).pathname))).flat(); const bad = []
const forbidden = /(bg|text|border|ring|fill|stroke|from|via|to|divide|outline)-(red|green|emerald|amber|yellow|orange|purple|violet|pink|rose|teal|cyan|sky|lime|indigo|fuchsia)-\d/g
for (const file of files) if (/\.(ts|tsx|js|jsx|html|css)$/.test(file)) { const value = await readFile(file, 'utf8'); if (forbidden.test(value)) bad.push(file) }
if (bad.length) { console.error(bad.join('\n')); process.exit(1) } console.log('Palette guard passed.')
