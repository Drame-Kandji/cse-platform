const UPPER = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
const LOWER = 'abcdefghijkmnopqrstuvwxyz'
const DIGITS = '23456789'
const SYMBOLS = '@#$%&*!?-_+='
const ALL = UPPER + LOWER + DIGITS + SYMBOLS

/** Longueur minimale d'un mot de passe temporaire. */
export const PASSWORD_MIN_LENGTH = 12

function randomIndex(max: number): number {
  const buffer = new Uint32Array(1)
  crypto.getRandomValues(buffer)
  return (buffer[0] ?? 0) % max
}

function pick(chars: string): string {
  return chars.charAt(randomIndex(chars.length))
}

/**
 * Genere un mot de passe temporaire robuste (aleatoire cryptographique, caracteres ambigus exclus),
 * contenant au moins une majuscule, une minuscule, un chiffre et un symbole.
 * @param length longueur (16 par defaut, minimum 12)
 */
export function generatePassword(length = 16): string {
  const size = Math.max(PASSWORD_MIN_LENGTH, length)
  const chars = [pick(UPPER), pick(LOWER), pick(DIGITS), pick(SYMBOLS)]
  while (chars.length < size) chars.push(pick(ALL))
  for (let i = chars.length - 1; i > 0; i -= 1) {
    const j = randomIndex(i + 1)
    const tmp = chars[i] ?? ''
    chars[i] = chars[j] ?? ''
    chars[j] = tmp
  }
  return chars.join('')
}

/** Regles de robustesse (affichees et verifiees par le schema). */
export const PASSWORD_RULES = [
  {
    test: (v: string) => v.length >= PASSWORD_MIN_LENGTH,
    message: `Au moins ${PASSWORD_MIN_LENGTH} caractères`,
  },
  { test: (v: string) => /[A-Z]/.test(v), message: 'Au moins une majuscule' },
  { test: (v: string) => /[a-z]/.test(v), message: 'Au moins une minuscule' },
  { test: (v: string) => /\d/.test(v), message: 'Au moins un chiffre' },
  { test: (v: string) => /[^A-Za-z0-9]/.test(v), message: 'Au moins un caractère spécial' },
] as const
