/**
 * Seuil d'alerte du score de performance fournisseur (0..100), miroir de
 * `cse.fournisseurs.score.seuil-alerte` (ms-fournisseurs, defaut 40) : en dessous, le score est en rouge.
 */
export const SEUIL_ALERTE_SCORE = 40

/** Vrai si le score est sous le seuil d'alerte. */
export function scoreSousSeuil(score: number | null | undefined): boolean {
  return typeof score === 'number' && score < SEUIL_ALERTE_SCORE
}
