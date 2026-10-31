import { readFile } from 'node:fs/promises'
import { walk } from './walk.mjs'

// Charte bleu + blanc : interdit les couleurs Tailwind brutes (y compris bleus et gris : passer par les
// jetons primary, foreground, muted, border, danger, chart-n) et les hex en dur dans les composants.
const forbidden =
  /(bg|text|border|ring|fill|stroke|from|via|to|divide|outline|shadow|decoration|caret)-(red|green|emerald|amber|yellow|orange|purple|violet|pink|rose|teal|cyan|sky|lime|indigo|fuchsia|blue|slate|gray|zinc|neutral|stone)-\d/
// Hors feuilles de jetons (index.css) : aucune couleur hexadecimale dans le code des interfaces.
const hex = /(?:fill|stroke|color|background|stopColor)\s*[:=]\s*['"{]*#[0-9a-f]{3,8}\b/i
const files = await walk(new URL('../apps', import.meta.url).pathname)
const bad = []
for (const file of files) {
  if (!/\.(ts|tsx|js|jsx|html|css)$/.test(file)) continue
  const source = await readFile(file, 'utf8')
  if (forbidden.test(source) || (/\.(tsx|jsx)$/.test(file) && hex.test(source))) bad.push(file)
}
if (bad.length) {
  console.error(bad.join('\n'))
  process.exit(1)
}
console.log(`Palette guard passed (${files.length} fichiers).`)
