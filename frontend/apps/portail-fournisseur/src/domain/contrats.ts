import { contratFacture } from './factures'
import type { Contrat, Facture, StatutContrat } from './types'

/** Etapes affichees au fournisseur (barre de progression du detail). */
export const ETAPES_CONTRAT = [
  'Signature',
  'Engagement',
  'Expédition',
  'Livraison',
  'Facturation',
  'Clôture',
] as const

/**
 * Index de l'etape courante (0 = Signature). CLOTURE = toutes terminees (6).
 * Un contrat LIVRE deja facture attend la cloture (5).
 */
export function etapeCourante(statut: StatutContrat, facture: boolean): number {
  switch (statut) {
    case 'BROUILLON':
    case 'EN_ATTENTE_SIGNATURE':
      return 0
    case 'SIGNE':
      return 1
    case 'ENGAGEMENT_VALIDE':
      return 2
    case 'EN_LIVRAISON':
      return 3
    case 'LIVRE':
      return facture ? 5 : 4
    case 'CLOTURE':
      return ETAPES_CONTRAT.length
  }
}

export type ActionContrat = 'SIGNER' | 'REFUSER' | 'EXPEDIER' | 'JALON' | 'FACTURER'

/**
 * Actions possibles du fournisseur selon le statut du contrat (§13) :
 * EN_ATTENTE_SIGNATURE : signer ou refuser ; ENGAGEMENT_VALIDE : declarer l'expedition ;
 * EN_LIVRAISON : renseigner les jalons restants ; LIVRE sans facture active : facturer (CLOTURE est solde).
 */
export function actionsContrat(contrat: Contrat, factures: readonly Facture[]): ActionContrat[] {
  switch (contrat.statut) {
    case 'EN_ATTENTE_SIGNATURE':
      return ['SIGNER', 'REFUSER']
    case 'ENGAGEMENT_VALIDE':
      return ['EXPEDIER']
    case 'EN_LIVRAISON':
      return contrat.jalons.some((j) => !j.dateReelle) ? ['JALON'] : []
    case 'LIVRE':
      return contratFacture(contrat.id, factures) ? [] : ['FACTURER']
    case 'CLOTURE':
    case 'BROUILLON':
    case 'SIGNE':
      return []
  }
}

export const ONGLETS_CONTRAT = ['A_SIGNER', 'EN_COURS', 'LIVRES', 'CLOTURES'] as const
export type OngletContrat = (typeof ONGLETS_CONTRAT)[number]

const STATUTS_PAR_ONGLET: Readonly<Record<OngletContrat, readonly StatutContrat[]>> = {
  A_SIGNER: ['EN_ATTENTE_SIGNATURE'],
  EN_COURS: ['SIGNE', 'ENGAGEMENT_VALIDE', 'EN_LIVRAISON'],
  LIVRES: ['LIVRE'],
  CLOTURES: ['CLOTURE'],
}

/** Contrats d'un onglet (`null` = Tous). */
export function contratsOnglet(contrats: readonly Contrat[], onglet: OngletContrat | null): Contrat[] {
  if (onglet === null) return [...contrats]
  const statuts = STATUTS_PAR_ONGLET[onglet]
  return contrats.filter((c) => statuts.includes(c.statut))
}

/** Contrats actifs (KPI accueil) : ni en preparation, ni clotures. */
export function contratsActifs(contrats: readonly Contrat[]): number {
  return contrats.filter((c) => c.statut !== 'BROUILLON' && c.statut !== 'CLOTURE').length
}
