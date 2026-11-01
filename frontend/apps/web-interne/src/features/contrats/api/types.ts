import type { CategorieMateriel, StatutApprobation, StatutContrat, StatutJalon } from '@/shared/domain/labels'

/** Clauses contractuelles (contrat d'API §5). */
export interface ClausesContrat {
  readonly garantieMois?: number | null
  readonly conditionsMaintenance?: string | null
  readonly conditionsLivraison?: string | null
  readonly penaliteRetardPctParJour?: number | null
}

/** Engagement budgetaire attache au contrat (valide par le DAF, R06). */
export interface EngagementContrat {
  readonly statut: StatutApprobation
  readonly montant: number
  readonly parNom?: string | null
  readonly dateDemande: string
  readonly dateDecision?: string | null
  readonly commentaire?: string | null
}

/** Jalon de livraison. */
export interface Jalon {
  readonly id: string
  readonly libelle: string
  readonly datePrevue: string
  readonly dateReelle?: string | null
  readonly statut: StatutJalon
}

/** Signature horodatee (CSE par le RA, puis fournisseur sur le portail). */
export interface SignatureContrat {
  readonly parNom: string
  readonly date: string
}

/** Contrat / bon de commande (ms-achats). */
export interface Contrat {
  readonly id: string
  readonly numero: string
  readonly appelOffreId: string
  readonly appelOffreReference: string
  readonly offreId: string
  readonly fournisseurId: string
  readonly fournisseurNom: string
  readonly besoinId?: string | null
  readonly chantierId?: string | null
  readonly chantierNom?: string | null
  readonly objet: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly quantite: number
  readonly montant: number
  readonly statut: StatutContrat
  readonly dateSignature?: string | null
  readonly delaiLivraisonJours: number
  readonly dateLivraisonPrevue?: string | null
  readonly dateLivraisonReelle?: string | null
  readonly numeroBonLivraison?: string | null
  readonly clauses: ClausesContrat
  readonly engagement?: EngagementContrat | null
  readonly jalons: readonly Jalon[]
  readonly penalitesRetard: number
  readonly signatureCse?: SignatureContrat | null
  readonly signatureFournisseur?: SignatureContrat | null
  /** Motif du dernier refus de signature du fournisseur (contrat revenu en BROUILLON). */
  readonly motifRefusSignature?: string | null
  readonly dateExpedition?: string | null
  readonly commentaireExpedition?: string | null
  readonly version: number
}

/** Filtres de la liste `GET /api/contrats`. */
export interface ContratFiltres {
  readonly statut?: StatutContrat
  readonly fournisseurId?: string
  readonly q?: string
}

/** Corps de `PUT /api/contrats/{id}/clauses`. */
export interface ClausesRequete extends ClausesContrat {
  readonly delaiLivraisonJours: number
  readonly jalons: readonly { readonly libelle: string; readonly datePrevue: string }[]
}

/** Corps de `POST /api/contrats/{id}/livraison/confirmer`. */
export interface ConfirmationLivraison {
  readonly dateLivraisonReelle: string
  readonly numeroBonLivraison: string
  readonly observations?: string
}

/** Fournisseur minimal pour le filtre de liste. */
export interface FournisseurOption {
  readonly id: string
  readonly raisonSociale: string
}
