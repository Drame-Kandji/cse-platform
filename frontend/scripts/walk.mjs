import { readdir } from 'node:fs/promises'
import { join } from 'node:path'

const IGNORED = new Set(['node_modules', 'dist', 'coverage', '.vite'])

/** Liste recursivement les fichiers sources (dossiers generes exclus). */
export async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true })
  const nested = await Promise.all(
    entries.map((entry) => {
      const path = join(dir, entry.name)
      if (entry.isDirectory()) return IGNORED.has(entry.name) ? [] : walk(path)
      return [path]
    }),
  )
  return nested.flat()
}
