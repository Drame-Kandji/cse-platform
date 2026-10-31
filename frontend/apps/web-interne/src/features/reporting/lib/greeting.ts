/** Salutation selon l'heure locale. */
export function greeting(date = new Date()): string {
  const hour = date.getHours()
  return hour >= 18 || hour < 4 ? 'Bonsoir' : 'Bonjour'
}
