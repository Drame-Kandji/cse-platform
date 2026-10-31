import { readFile } from 'node:fs/promises'
import { walk } from './walk.mjs'

// Interdit les couleurs Tailwind brutes : utiliser les jetons semantiques (primary, success, chart-1...).
const forbidden =
  /(bg|text|border|ring|fill|stroke|from|via|to|divide|outline|shadow|decoration|caret)-(red|green|emerald|amber|yellow|orange|purple|violet|pink|rose|teal|cyan|sky|lime|indigo|fuchsia)-\d/
const files = await walk(new URL('../apps', import.meta.url).pathname)
const bad = []
for (const file of files) {
  if (/\.(ts|tsx|js|jsx|html|css)$/.test(file) && forbidden.test(await readFile(file, 'utf8'))) bad.push(file)
}
if (bad.length) {
  console.error(bad.join('\n'))
  process.exit(1)
}
console.log(`Palette guard passed (${files.length} fichiers).`)
