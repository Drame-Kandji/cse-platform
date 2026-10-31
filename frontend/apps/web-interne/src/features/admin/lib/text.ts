/** Normalise un texte pour une recherche locale (minuscules, sans accents). */
export function normalizeText(value: string): string {
  return value.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}
