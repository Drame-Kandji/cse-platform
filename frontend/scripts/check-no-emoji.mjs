import { readdir, readFile } from 'node:fs/promises'
import { join } from 'node:path'
const walk = async (dir) => (await readdir(dir, { withFileTypes: true })).flatMap(async (entry) => entry.isDirectory() ? walk(join(dir, entry.name)) : [join(dir, entry.name)])
const files = (await Promise.all(await walk(new URL('../apps', import.meta.url).pathname))).flat()
const offenders = []
for (const file of files) if (/\.(ts|tsx|js|jsx|html|css)$/.test(file) && /\p{Extended_Pictographic}/u.test(await readFile(file, 'utf8'))) offenders.push(file)
if (offenders.length) { console.error(offenders.join('\n')); process.exit(1) }
console.log('No emoji found.')
