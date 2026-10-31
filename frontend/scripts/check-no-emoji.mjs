import { readFile } from 'node:fs/promises'
import { walk } from './walk.mjs'

// Interdit tout emoji dans le code source des applications.
const files = await walk(new URL('../apps', import.meta.url).pathname)
const offenders = []
for (const file of files) {
  if (/\.(ts|tsx|js|jsx|html|css|json)$/.test(file) && /\p{Extended_Pictographic}/u.test(await readFile(file, 'utf8'))) offenders.push(file)
}
if (offenders.length) {
  console.error(offenders.join('\n'))
  process.exit(1)
}
console.log(`No emoji found (${files.length} fichiers).`)
