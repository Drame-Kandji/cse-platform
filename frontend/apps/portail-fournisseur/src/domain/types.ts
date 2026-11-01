import type {
  CategorieMateriel,
  CritereEvaluation,
  StatutApprobation,
  StatutAO,
  StatutFacture,
  StatutFournisseur,
  StatutJalon,
  TypeAppelOffre,
  TypeDocument,
} from '@/shared/domain/labels'

/**
 * DTO du portail fournisseur (contrat d'API §13, §5 et §4), miroir des records Java.
 * Les champs ajoutes par §13 mais pas encore servis par le backend sont optionnels.
 */

export const STATUT_CONTRAT_PORTAIL = [
  'BROUILLON',
  'EN_ATTENTE_SIGNATURE',
  'SIGNE',
  'ENGAGEMENT_VALIDE',
  'EN_LIVRAISON',
  'LIVRE',
  'CLOTURE',
] as const
/** StatutContrat complet (§1), y compris EN_ATTENTE_SIGNATURE propre au circuit portail. */
export type StatutContrat = (typeof STATUT_CONTRAT_PORTAIL)[number]

export const RESULTAT_OFFRE = ['EN_COURS', 'RETENUE', 'NON_RETENUE'] as const
export type ResultatOffre = (typeof RESULTAT_OFFRE)[number]

export interface CriterePondere {
  readonly code: CritereEvaluation
  readonly poids: number
}

export interface AppelOffrePortail {
  readonly id: string
  readonly reference: string
  readonly objet: string
  readonly description?: string | null
  readonly type: TypeAppelOffre
  readonly categorie: CategorieMateriel
  readonly quantite: number
  readonly specifications?: string | null
  readonly datePublication: string
  readonly dateLimite: string
  readonly statut: StatutAO
  readonly criteres: readonly CriterePondere[]
  readonly chantierNom?: string | null
  /** §13 ; absent tant que le backend ne le sert pas (deduit du type, voir `estInvite`). */
  readonly invite?: boolean
  readonly offreDeposee: boolean
  readonly monOffreId?: string | null
}

export interface OffrePortail {
  readonly id: string
  readonly appelOffreId: string
  readonly appelOffreReference: string
  readonly appelOffreObjet: string
  readonly montantPropose: number
  readonly delaiLivraisonJours: number
  readonly garantieMois?: number | null
  readonly description?: string | null
  readonly dateDepot: string
  readonly statutAppelOffre: StatutAO
  readonly dateLimite?: string | null
  readonly codeAnonyme?: string | null
  /** §13 ; a defaut, deduit de `statutAppelOffre` et `retenue` (voir `resultatOffre`). */
  readonly resultat?: ResultatOffre
  readonly retenue?: boolean
}

export interface OffreDepot {
  readonly montantPropose: number
  readonly delaiLivraisonJours: number
  readonly garantieMois?: number
  readonly description?: string
}

export interface Jalon {
  readonly id: string
  readonly libelle: string
  readonly datePrevue: string
  readonly dateReelle?: string | null
  readonly statut: StatutJalon
}

export interface Signature {
  readonly parNom: string
  readonly date: string
}

export interface Contrat {
  readonly id: string
  readonly numero: string
  readonly appelOffreId: string
  readonly appelOffreReference: string
  readonly offreId: string
  readonly fournisseurId: string
  readonly fournisseurNom: string
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
  readonly clauses: {
    readonly garantieMois?: number | null
    readonly conditionsMaintenance?: string | null
    readonly conditionsLivraison?: string | null
    readonly penaliteRetardPctParJour?: number | null
  }
  readonly engagement?: {
    readonly statut: StatutApprobation
    readonly montant: number
    readonly dateDemande: string
    readonly dateDecision?: string | null
  } | null
  readonly jalons: readonly Jalon[]
  readonly penalitesRetard: number
  readonly signatureCse?: Signature | null
  readonly signatureFournisseur?: Signature | null
  readonly motifRefusSignature?: string | null
  readonly dateExpedition?: string | null
  readonly commentaireExpedition?: string | null
  readonly version: number
}

export interface Facture {
  readonly id: string
  readonly numero: string
  readonly contratId: string
  readonly contratNumero: string
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

export interface FactureDepot {
  readonly contratId: string
  readonly numero: string
  readonly montantHt: number
  /** Montant de TVA en FCFA (et non un taux). */
  readonly tva: number
  readonly dateEmission: string
  readonly dateEcheance: string
}

export interface DocumentAdministratif {
  readonly id: string
  readonly type: TypeDocument
  readonly libelle: string
  readonly dateExpiration?: string | null
  readonly expire: boolean
}

export interface CoordonneesBancaires {
  readonly banque: string
  readonly iban: string
}

export interface ProfilFournisseur {
  readonly id: string
  readonly raisonSociale: string
  readonly ninea: string
  readonly rccm?: string | null
  readonly adresse?: string | null
  readonly ville?: string | null
  readonly pays: string
  readonly telephone?: string | null
  readonly email: string
  readonly contactNom?: string | null
  readonly contactFonction?: string | null
  readonly specialites: readonly CategorieMateriel[]
  readonly certifications: readonly string[]
  readonly statut: StatutFournisseur
  readonly motifStatut?: string | null
  readonly scorePerformance: number
  readonly nombreEvaluations: number
  readonly tauxLivraisonALHeure?: number | null
  readonly dateInscription: string
  readonly documents: readonly DocumentAdministratif[]
  readonly coordonneesBancaires?: CoordonneesBancaires | null
  readonly version: number
}

/** Corps de `PUT /api/portail/profil` (ModifierFournisseurRequest). */
export interface ProfilModification {
  readonly raisonSociale: string
  readonly ninea: string
  readonly rccm?: string
  readonly adresse?: string
  readonly ville?: string
  readonly pays: string
  readonly telephone?: string
  readonly email: string
  readonly contactNom?: string
  readonly contactFonction?: string
  readonly specialites: readonly CategorieMateriel[]
  readonly certifications: readonly string[]
  readonly coordonneesBancaires?: CoordonneesBancaires
  readonly version: number
}

export interface LigneCatalogue {
  readonly id: string
  readonly reference: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly prixUnitaire: number
  readonly unite: string
  readonly delaiLivraisonJours: number
  readonly dateMaj: string
}

/** Corps de `PUT /api/portail/catalogue` (LigneCatalogueRequest, upsert par reference). */
export type LigneCatalogueSaisie = Omit<LigneCatalogue, 'id' | 'dateMaj'>

/** Corps de `POST /api/portail/inscription` (public). */
export interface Inscription {
  readonly raisonSociale: string
  readonly ninea: string
  readonly rccm?: string
  readonly adresse?: string
  readonly ville?: string
  readonly pays: string
  readonly telephone?: string
  readonly email: string
  readonly contactNom: string
  readonly contactFonction?: string
  readonly specialites: readonly CategorieMateriel[]
}

export interface InscriptionRecue {
  readonly id: string
  readonly raisonSociale: string
  readonly statut: StatutFournisseur
}
