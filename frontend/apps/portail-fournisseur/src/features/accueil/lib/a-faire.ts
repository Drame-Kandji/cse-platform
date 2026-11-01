import { to } from '@/app/routes'
import { estInvite } from '@/domain/offres'
import { contratFacture } from '@/domain/factures'
import type { AppelOffrePortail, Contrat, Facture } from '@/domain/types'

export type TypeAFaire = 'OFFRE' | 'SIGNATURE' | 'EXPEDITION' | 'FACTURE' | 'FACTURE_REJETEE'

export interface AFaire {
  readonly id: string
  readonly type: TypeAFaire
  /** Action courte : « Signer le contrat ». */
  readonly titre: string
  /** Reference et objet. */
  readonly detail: string
  readonly lien: string
  /** Echeance utile au tri (date limite, date prevue). */
  readonly echeance?: string | null
}

/** Ordre d'affichage : ce qui bloque la CSE d'abord. */
const PRIORITE: Readonly<Record<TypeAFaire, number>> = {
  SIGNATURE: 0,
  EXPEDITION: 1,
  FACTURE_REJETEE: 2,
  FACTURE: 3,
  OFFRE: 4,
}

export interface SourcesAFaire {
  readonly appelsOffres: readonly AppelOffrePortail[]
  readonly contrats: readonly Contrat[]
  readonly factures: readonly Facture[]
}

/**
 * Liste « À faire » de l'accueil : invitations sans offre, contrats a signer, a expedier, livres sans
 * facture active (premier depot ou nouveau depot), factures rejetees non remplacees (vers l'onglet
 * Rejetées : le contrat n'est pas forcement livre). Triee par priorite puis echeance.
 */
export function deriverAFaire({ appelsOffres, contrats, factures }: SourcesAFaire): AFaire[] {
  const items: AFaire[] = []
  for (const ao of appelsOffres) {
    if (ao.statut === 'PUBLIE' && estInvite(ao) && !ao.offreDeposee) {
      items.push({
        id: `ao-${ao.id}`,
        type: 'OFFRE',
        titre: 'Déposer une offre',
        detail: `${ao.reference} · ${ao.objet}`,
        lien: to.appelOffreDetail(ao.id),
        echeance: ao.dateLimite,
      })
    }
  }
  for (const c of contrats) {
    const detail = `${c.numero} · ${c.objet}`
    if (c.statut === 'EN_ATTENTE_SIGNATURE') {
      items.push({
        id: `sig-${c.id}`,
        type: 'SIGNATURE',
        titre: 'Signer le contrat',
        detail,
        lien: to.contratDetail(c.id),
      })
    } else if (c.statut === 'ENGAGEMENT_VALIDE') {
      items.push({
        id: `exp-${c.id}`,
        type: 'EXPEDITION',
        titre: "Déclarer l'expédition",
        detail,
        lien: to.contratDetail(c.id),
        echeance: c.dateLivraisonPrevue,
      })
    } else if (c.statut === 'LIVRE' && !contratFacture(c.id, factures)) {
      items.push({
        id: `fac-${c.id}`,
        type: 'FACTURE',
        titre: 'Déposer la facture',
        detail,
        lien: to.nouvelleFacture(c.id),
      })
    }
  }
  const relances = new Set<string>()
  for (const f of factures) {
    if (f.statut === 'REJETEE' && !relances.has(f.contratId) && !contratFacture(f.contratId, factures)) {
      relances.add(f.contratId)
      items.push({
        id: `rej-${f.id}`,
        type: 'FACTURE_REJETEE',
        titre: 'Facture rejetée',
        detail: f.motifRejet ? `${f.numero} · ${f.motifRejet}` : f.numero,
        lien: to.factures('REJETEE'),
      })
    }
  }
  return items.sort(
    (a, b) => PRIORITE[a.type] - PRIORITE[b.type] || (a.echeance ?? '9').localeCompare(b.echeance ?? '9'),
  )
}
