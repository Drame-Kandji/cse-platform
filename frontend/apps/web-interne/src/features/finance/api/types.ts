import type { StatutApprobation, StatutContrat, StatutFacture } from '@/shared/domain/labels'

/** Engagement budgetaire d'un contrat signe (contrat d'API §5). */
export interface Engagement {
  readonly contratId: string
  readonly contratNumero: string
  readonly fournisseurNom: string
  readonly chantierNom?: string | null
  readonly objet: string
  readonly montant: number
  readonly statut: StatutApprobation
  readonly dateDemande: string
  readonly dateDecision?: string | null
  readonly parNom?: string | null
}

/** Facture fournisseur (contrat d'API §5). */
export interface Facture {
  readonly id: string
  readonly numero: string
  readonly contratId: string
  readonly contratNumero: string
  readonly fournisseurId: string
  readonly fournisseurNom: string
  readonly montantHt: number
  readonly tva: number
  readonly montantTtc: number
  readonly dateEmission: string
  readonly dateEcheance: string
  readonly statut: StatutFacture
  readonly referenceMandat?: string | null
  readonly dateMandatement?: string | null
  readonly datePaiement?: string | null
  readonly motifRejet?: string | null
  readonly enRetard: boolean
}

/** Synthese financiere (DAF, DG). */
export interface SyntheseFinance {
  readonly montantEngage: number
  readonly montantEngagementsEnAttente: number
  readonly montantFacture: number
  readonly montantMandate: number
  readonly montantPaye: number
  readonly facturesEnRetard: number
  readonly engagementsEnAttente: number
}

/** Projection minimale d'un contrat pour le choix lors de la saisie d'une facture. */
export interface ContratResume {
  readonly id: string
  readonly numero: string
  readonly fournisseurNom: string
  readonly objet: string
  readonly montant: number
  readonly statut: StatutContrat
}

export interface EngagementFiltres {
  readonly statut?: StatutApprobation
}

export interface FactureFiltres {
  readonly statut?: StatutFacture
  readonly enRetard?: boolean
}

/** Corps de POST /engagements/{contratId}/valider. */
export interface DecisionEngagement {
  readonly approuve: boolean
  readonly commentaire?: string
}

/** Corps de POST /factures. */
export interface FactureCreation {
  readonly contratId: string
  readonly numero: string
  readonly montantHt: number
  readonly tva: number
  readonly dateEmission: string
  readonly dateEcheance: string
}
