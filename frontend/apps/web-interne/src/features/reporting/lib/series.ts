import type { Indicateur, Tableau } from '../types'

/** Series utiles selon les sections presentes du tableau. */
export function seriesFor(tableau: Tableau | undefined): Indicateur[] {
  if (!tableau) return []
  const indicateurs: Indicateur[] = []
  if (tableau.parc) indicateurs.push('TAUX_DISPONIBILITE', 'TAUX_UTILISATION')
  if (tableau.finances) indicateurs.push('MONTANT_ENGAGE', 'COUT_MAINTENANCE')
  if (tableau.besoins) indicateurs.push('BESOINS_SOUMIS')
  if (tableau.achats) indicateurs.push('DELAI_APPROVISIONNEMENT')
  return indicateurs
}
