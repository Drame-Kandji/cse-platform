import type { CategorieMateriel, Priorite } from '@/shared/domain/labels'

/** Profils de tableau de bord (contrat §6 ; `DG` accepte aussi l'alias `DIRECTION_GENERALE`). */
export const PROFILS = ['DG', 'DAF', 'RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'CHEF_CHANTIER'] as const
export type Profil = (typeof PROFILS)[number]

export const INDICATEURS = [
  'TAUX_DISPONIBILITE',
  'TAUX_UTILISATION',
  'MONTANT_ENGAGE',
  'COUT_MAINTENANCE',
  'BESOINS_SOUMIS',
  'DELAI_APPROVISIONNEMENT',
] as const
export type Indicateur = (typeof INDICATEURS)[number]

export const GRANULARITES = ['JOUR', 'SEMAINE', 'MOIS'] as const
export type Granularite = (typeof GRANULARITES)[number]

/** Taux exprimes en pourcentage 0..100 (une decimale) par ms-reporting. */
export type Pourcentage = number

export interface ParcCategorie {
  readonly categorie: CategorieMateriel
  readonly total: number
  readonly disponibles: number
  readonly affectes: number
  readonly enMaintenance: number
  readonly tauxUtilisation: Pourcentage
}

export interface SectionParc {
  readonly total: number
  readonly disponibles: number
  readonly affectes: number
  readonly enMaintenance: number
  readonly reformes: number
  readonly tauxDisponibilite: Pourcentage
  readonly tauxUtilisation: Pourcentage
  readonly maintenancesEnCours: number
  readonly pannesOuvertes: number
  readonly alertesMaintenance: number
  readonly parCategorie: readonly ParcCategorie[]
}

export interface SectionBesoins {
  readonly total: number
  readonly enAttente: number
  readonly valides: number
  readonly rejetes: number
  readonly affectesDirect: number
  readonly enAchat: number
  readonly delaiMoyenTraitementHeures?: number | null
  readonly parPriorite: Readonly<Partial<Record<Priorite, number>>>
}

export interface SectionAchats {
  readonly aoPublies: number
  readonly aoClotures: number
  readonly aoAttribues: number
  readonly contratsEnCours: number
  readonly livraisonsEnRetard: number
  readonly delaiMoyenApprovisionnementJours?: number | null
  readonly tauxLivraisonALHeure?: Pourcentage | null
}

export interface TopFournisseur {
  readonly fournisseurId: string
  readonly raisonSociale: string
  /** Score 0..100. */
  readonly scorePerformance: number
  readonly nombreLitiges: number
  readonly tauxLivraisonALHeure?: Pourcentage | null
}

export interface SectionFournisseurs {
  readonly actifs: number
  readonly scoreMoyen: number
  readonly litigesOuverts: number
  readonly top: readonly TopFournisseur[]
}

export interface SectionFinances {
  readonly budgetTotal: number
  readonly montantEngage: number
  readonly montantMandate: number
  readonly montantPaye: number
  readonly tauxExecutionBudget: Pourcentage
  readonly coutMaintenance: number
  readonly coutExploitation: number
}

export interface LigneChantier {
  readonly chantierId: string
  readonly chantierNom: string
  readonly region: string
  readonly budget: number
  readonly montantMarche: number
  readonly montantEngage: number
  readonly coutExploitation: number
  readonly coutMaintenance: number
  readonly marge: number
  readonly materielsAffectes: number
  readonly tauxUtilisation: Pourcentage
  readonly besoinsEnAttente: number
}

/** Tableau de bord d'un profil : seules les sections du profil sont presentes. */
export interface Tableau {
  readonly profil: Profil
  readonly du: string
  readonly au: string
  readonly genereLe: string
  readonly parc?: SectionParc | null
  readonly besoins?: SectionBesoins | null
  readonly achats?: SectionAchats | null
  readonly fournisseurs?: SectionFournisseurs | null
  readonly finances?: SectionFinances | null
  readonly chantiers?: readonly LigneChantier[] | null
}

export interface Point {
  /** "2026-07" | "2026-07-14" | "2026-W29". */
  readonly periode: string
  readonly valeur: number
}

export interface TableauParams {
  readonly profil?: Profil
  readonly du?: string
  readonly au?: string
  readonly chantierId?: string
}

export interface SeriesParams {
  readonly indicateur: Indicateur
  readonly granularite: Granularite
  readonly du?: string
  readonly au?: string
  readonly chantierId?: string
}

export type ExportFormat = 'pdf' | 'xlsx'
