import { paths, to } from '@/app/routes'
import type { Source } from '../types'

/**
 * Route interne d'une source citee par l'assistant : le `lien` fourni s'il est interne,
 * sinon une route deduite du type d'objet.
 */
export function sourceLink(source: Source): string {
  const lien = source.lien
  if (lien?.startsWith('/') && !lien.startsWith('//')) return lien
  switch (source.type) {
    case 'MATERIEL':
      return to.materielDetail(source.id)
    case 'BESOIN':
      return to.besoinDetail(source.id)
    case 'FOURNISSEUR':
      return to.fournisseurDetail(source.id)
    case 'APPEL_OFFRE':
      return to.appelOffreDetail(source.id)
    case 'CONTRAT':
      return to.contratDetail(source.id)
    case 'CHANTIER':
      return paths.chantiers
    case 'MAINTENANCE':
      return paths.maintenances
    default:
      return paths.dashboard
  }
}

/** Libelle francais du type de source. */
export const SOURCE_TYPE_LABELS: Readonly<Record<Source['type'], string>> = {
  MATERIEL: 'Matériel',
  BESOIN: 'Besoin',
  CHANTIER: 'Chantier',
  CONTRAT: 'Contrat',
  FOURNISSEUR: 'Fournisseur',
  MAINTENANCE: 'Maintenance',
  APPEL_OFFRE: "Appel d'offres",
}
