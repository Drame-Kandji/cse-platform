import type {
  CategorieMateriel,
  GraviteLitige,
  StatutFournisseur,
  StatutLitige,
  TypeDocument,
} from '@/shared/domain/labels'

/** DTO ms-fournisseurs (contrat d'API §4). */
export interface DemandeBlacklistage {
  readonly demandeurNom: string
  readonly motif: string
  readonly date: string
}

export interface Fournisseur {
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
  /** 0..100 */
  readonly scorePerformance: number
  readonly nombreEvaluations: number
  /** 0..100 */
  readonly tauxLivraisonALHeure?: number | null
  readonly nombreLitigesOuverts: number
  readonly documentsExpires: number
  readonly dateInscription: string
  readonly blacklistageDemande?: DemandeBlacklistage | null
  readonly version: number
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

/** Detail : coordonnees bancaires renvoyees uniquement aux roles RA et DAF. */
export interface FournisseurDetail extends Fournisseur {
  readonly documents: readonly DocumentAdministratif[]
  readonly coordonneesBancaires?: CoordonneesBancaires | null
}

export interface Evaluation {
  readonly id: string
  readonly contratId?: string | null
  /** Notes entieres 0..10 */
  readonly qualite: number
  readonly delais: number
  readonly conformite: number
  readonly sav: number
  readonly prix: number
  /** 0..100 */
  readonly noteGlobale: number
  readonly commentaire?: string | null
  readonly evaluateurNom: string
  readonly date: string
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

export interface PrixHistorique {
  readonly reference: string
  readonly designation: string
  readonly prixUnitaire: number
  readonly date: string
}

export interface Classement {
  readonly rang: number
  readonly fournisseurId: string
  readonly raisonSociale: string
  readonly scorePerformance: number
  readonly nombreEvaluations: number
  readonly tauxLivraisonALHeure?: number | null
  readonly nombreLitiges: number
  readonly specialites: readonly CategorieMateriel[]
}

export interface OffrePrix {
  readonly fournisseurId: string
  readonly raisonSociale: string
  readonly prixUnitaire: number
  readonly delaiLivraisonJours: number
  readonly scorePerformance: number
}

export interface ComparaisonPrix {
  readonly designation: string
  readonly reference?: string | null
  readonly categorie: CategorieMateriel
  readonly prixMin: number
  readonly prixMax: number
  readonly prixMoyen: number
  readonly offres: readonly OffrePrix[]
}

export type TypeActionLitige = 'OUVERTURE' | 'ACTION' | 'RESOLUTION' | 'CLASSEMENT'

export interface ActionLitige {
  readonly date: string
  readonly auteurNom: string
  readonly type: TypeActionLitige
  readonly commentaire: string
}

export interface Litige {
  readonly id: string
  readonly reference: string
  readonly fournisseurId: string
  readonly raisonSociale: string
  readonly contratId?: string | null
  readonly objet: string
  readonly description: string
  readonly gravite: GraviteLitige
  readonly statut: StatutLitige
  readonly dateOuverture: string
  readonly dateResolution?: string | null
  readonly actions: readonly ActionLitige[]
}

export interface StatistiquesFournisseurs {
  readonly total: number
  readonly actifs: number
  readonly enAttente: number
  readonly suspendus: number
  readonly blacklistes: number
  readonly scoreMoyen: number
  readonly litigesOuverts: number
  readonly documentsExpires: number
}

/* ---- Corps de requete ---- */

export interface FournisseurFiltres {
  readonly q?: string
  readonly specialite?: CategorieMateriel
  readonly statut?: StatutFournisseur
}

export interface FournisseurSaisie {
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
}

export interface FournisseurModification extends FournisseurSaisie {
  readonly version: number
}

export interface DocumentSaisie {
  readonly type: TypeDocument
  readonly libelle: string
  readonly dateExpiration?: string
}

export interface EvaluationSaisie {
  readonly contratId?: string
  readonly qualite: number
  readonly delais: number
  readonly conformite: number
  readonly sav: number
  readonly prix: number
  readonly commentaire?: string
}

export interface LigneCatalogueSaisie {
  readonly reference: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly prixUnitaire: number
  readonly unite: string
  readonly delaiLivraisonJours: number
}

export interface LitigeSaisie {
  readonly contratId?: string
  readonly objet: string
  readonly description: string
  readonly gravite: GraviteLitige
}

export interface LitigeActionSaisie {
  readonly type: Exclude<TypeActionLitige, 'OUVERTURE'>
  readonly commentaire: string
}

export interface DecisionBlacklistage {
  readonly approuve: boolean
  readonly commentaire?: string
}
