import type { CategorieMateriel } from '@/shared/domain/labels'
import type { Affectation } from '@/shared/domain/types'

/** Ligne du planning : un materiel et ses affectations sur un chantier. */
export interface LignePlanning {
  readonly materielId: string
  readonly materielCode: string
  readonly materielDesignation: string
  readonly categorie: CategorieMateriel
  readonly affectations: readonly Affectation[]
}

/** Groupe de lignes par chantier. */
export interface GroupePlanning {
  readonly chantierId: string
  readonly chantierNom: string
  readonly lignes: readonly LignePlanning[]
}

/** Fin effective d'une affectation : fin reelle si connue, sinon fin prevue. */
export function finEffective(affectation: Pick<Affectation, 'dateFinPrevue' | 'dateFinReelle'>): string {
  return affectation.dateFinReelle ?? affectation.dateFinPrevue
}

/**
 * Regroupe les affectations par chantier puis par materiel (lignes du Gantt).
 * Un materiel affecte successivement a deux chantiers apparait dans les deux groupes.
 * Tri : chantiers par nom, materiels par code, affectations par date de debut.
 */
export function grouperPlanning(affectations: readonly Affectation[]): GroupePlanning[] {
  const chantiers = new Map<
    string,
    { nom: string; lignes: Map<string, { premiere: Affectation; items: Affectation[] }> }
  >()
  for (const affectation of affectations) {
    let chantier = chantiers.get(affectation.chantierId)
    if (!chantier) {
      chantier = { nom: affectation.chantierNom, lignes: new Map() }
      chantiers.set(affectation.chantierId, chantier)
    }
    const ligne = chantier.lignes.get(affectation.materielId)
    if (ligne) ligne.items.push(affectation)
    else chantier.lignes.set(affectation.materielId, { premiere: affectation, items: [affectation] })
  }
  return [...chantiers.entries()]
    .map(([chantierId, chantier]) => ({
      chantierId,
      chantierNom: chantier.nom,
      lignes: [...chantier.lignes.values()]
        .map(({ premiere, items }): LignePlanning => ({
          materielId: premiere.materielId,
          materielCode: premiere.materielCode,
          materielDesignation: premiere.materielDesignation,
          categorie: premiere.categorie,
          affectations: [...items].sort((a, b) => a.dateDebut.localeCompare(b.dateDebut)),
        }))
        .sort((a, b) => a.materielCode.localeCompare(b.materielCode, 'fr')),
    }))
    .sort((a, b) => a.chantierNom.localeCompare(b.chantierNom, 'fr'))
}
