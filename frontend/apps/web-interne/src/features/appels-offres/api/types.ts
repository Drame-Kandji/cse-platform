import type {
  CategorieMateriel,
  CritereEvaluation,
  Priorite,
  StatutAO,
  StatutApprobation,
  StatutFournisseur,
  TypeAppelOffre,
  TypeProcesVerbal,
} from '@/shared/domain/labels'

/** Critere pondere d'un appel d'offres (somme des poids = 100). */
export interface CriterePondere {
  readonly code: CritereEvaluation
  readonly poids: number
}

/** Decision d'une approbation (co-validation RM, approbation DG, R11). */
export interface Approbation {
  readonly statut: StatutApprobation
  readonly parNom?: string | null
  readonly date?: string | null
  readonly commentaire?: string | null
}

/** Proposition d'attribution d'un appel d'offres. */
export interface Attribution {
  readonly offreId: string
  readonly codeAnonyme: string
  readonly fournisseurNom: string
  readonly montant: number
  readonly justification: string
  readonly proposeeParNom: string
  readonly dateProposition: string
  readonly coValidationMateriel: Approbation
  /** Present uniquement si le montant depasse le seuil de delegation (R11). */
  readonly approbationDg?: Approbation | null
}

/** Appel d'offres (ms-achats, contrat §5). */
export interface AppelOffre {
  readonly id: string
  readonly reference: string
  readonly objet: string
  readonly description?: string | null
  readonly type: TypeAppelOffre
  readonly besoinId?: string | null
  readonly besoinReference?: string | null
  readonly chantierId?: string | null
  readonly chantierNom?: string | null
  readonly categorie: CategorieMateriel
  readonly quantite: number
  readonly specifications?: string | null
  readonly budgetEstime?: number | null
  readonly datePublication: string
  /** Instant ISO-8601 UTC. */
  readonly dateLimite: string
  readonly statut: StatutAO
  readonly criteres: readonly CriterePondere[]
  readonly fournisseursInvites: readonly { readonly id: string; readonly raisonSociale: string }[]
  readonly nombreOffres: number
  readonly offreRetenueId?: string | null
  readonly montantAttribue?: number | null
  readonly attribution?: Attribution | null
  readonly creeParNom: string
  readonly version: number
}

/** Notes par critere, 0..100. */
export type NotesOffre = Partial<Record<CritereEvaluation, number>>

/** Offre recue (anonymisee tant que l'AO est PUBLIE, R10). */
export interface Offre {
  readonly id: string
  readonly appelOffreId: string
  readonly codeAnonyme: string
  readonly fournisseurId?: string | null
  readonly fournisseurNom?: string | null
  readonly montantPropose: number
  readonly delaiLivraisonJours: number
  readonly garantieMois?: number | null
  readonly description?: string | null
  readonly dateDepot: string
  readonly notes: NotesOffre
  readonly scoreGlobal?: number | null
  readonly rang?: number | null
  readonly retenue: boolean
}

/** Comparatif multicritere (offres triees par rang). */
export interface Comparatif {
  readonly appelOffreId: string
  readonly criteres: readonly CriterePondere[]
  readonly offres: readonly Offre[]
  readonly recommandationOffreId?: string | null
}

/** Besoin valide oriente vers l'achat (projection BESOIN_EN_ACHAT). */
export interface BesoinAAcheter {
  readonly besoinId: string
  readonly reference: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly quantite: number
  readonly specifications?: string | null
  readonly priorite: Priorite
  readonly chantierId: string
  readonly chantierNom: string
  readonly dateReception: string
  readonly appelOffreId?: string | null
}

/** Fournisseur candidat a l'invitation (sous-ensemble de `Fournisseur`, contrat §4). */
export interface FournisseurInvitable {
  readonly id: string
  readonly raisonSociale: string
  readonly ville?: string | null
  readonly statut: StatutFournisseur
  readonly specialites: readonly CategorieMateriel[]
  readonly scorePerformance: number
  readonly nombreEvaluations: number
  readonly tauxLivraisonALHeure?: number | null
}

export interface AppelOffreFiltres {
  /** Filtres mono-valeur cote API (un parametre repete ne garde que le premier). */
  readonly statut?: StatutAO
  readonly type?: TypeAppelOffre
  readonly q?: string
}

/** Corps de POST /appels-offres. */
export interface AppelOffreCreation {
  readonly besoinId?: string
  readonly objet: string
  readonly description?: string
  readonly type: TypeAppelOffre
  readonly categorie: CategorieMateriel
  readonly quantite: number
  readonly specifications?: string
  readonly budgetEstime?: number
  readonly dateLimite: string
  readonly criteres: readonly CriterePondere[]
  readonly fournisseurIdsInvites: readonly string[]
}

/** Corps de POST /appels-offres/{id}/offres (offre recue hors portail). */
export interface OffreSaisie {
  readonly fournisseurId: string
  readonly montantPropose: number
  readonly delaiLivraisonJours: number
  readonly garantieMois?: number
  readonly description?: string
}

/** Criteres notes manuellement (PRIX et DELAI sont calcules). */
export const CRITERES_MANUELS = ['QUALITE_TECHNIQUE', 'GARANTIE', 'SAV', 'REFERENCES'] as const
export type CritereManuel = (typeof CRITERES_MANUELS)[number]
export type EvaluationOffre = Partial<Record<CritereManuel, number>>

/** Sous-ensemble de `Contrat` (§5) utile pour relier un AO attribue a son contrat. */
export interface ContratLie {
  readonly id: string
  readonly numero: string
  readonly appelOffreId: string
}

export interface DecisionApprobation {
  readonly approuve: boolean
  readonly commentaire?: string
}

export type { TypeProcesVerbal }
