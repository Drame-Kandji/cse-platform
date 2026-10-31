/**
 * DTO du service ms-suivi (chantiers, parc, affectations, maintenance) partages entre modules.
 * Miroir exact de `docs/API_CONTRACT.md` §2 : dates `LocalDate` en "yyyy-MM-dd", instants ISO-8601 UTC,
 * montants entiers en FCFA. Ne jamais dupliquer ces types dans un module.
 */
import type {
  CategorieMateriel,
  EtatMateriel,
  GravitePanne,
  StatutAffectation,
  StatutChantier,
  StatutMaintenance,
  StatutPanne,
  TypeMaintenance,
  TypeMouvement,
} from '../labels'

/** Date locale "yyyy-MM-dd". */
export type IsoDate = string
/** Instant ISO-8601 UTC. */
export type IsoInstant = string

export interface Chantier {
  readonly id: string
  readonly code: string
  readonly nom: string
  readonly localisation: string
  readonly region: string
  readonly latitude: number
  readonly longitude: number
  readonly statut: StatutChantier
  readonly budget: number
  readonly montantMarche: number
  readonly dateDebut: IsoDate
  readonly dateFinPrevue?: IsoDate | null
  readonly chefChantierId?: string | null
  readonly chefChantierNom?: string | null
  readonly nombreMateriels: number
  readonly version: number
}

export interface Materiel {
  readonly id: string
  readonly code: string
  readonly immatriculation?: string | null
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly typeEnginId?: string | null
  readonly typeEnginLibelle?: string | null
  readonly marque?: string | null
  readonly modele?: string | null
  readonly etat: EtatMateriel
  readonly heuresFonctionnement: number
  readonly heuresDepuisMaintenance: number
  readonly seuilMaintenanceHeures?: number | null
  readonly prochaineMaintenance?: IsoDate | null
  readonly maintenanceDue: boolean
  readonly tarifJournalier: number
  readonly valeurAcquisition?: number | null
  readonly dateAcquisition?: IsoDate | null
  readonly chantierActuelId?: string | null
  readonly chantierActuelNom?: string | null
  readonly latitude?: number | null
  readonly longitude?: number | null
  readonly fournisseurId?: string | null
  readonly contratId?: string | null
  readonly reformeDemandee: boolean
  readonly motifReforme?: string | null
  readonly version: number
}

export interface Affectation {
  readonly id: string
  readonly materielId: string
  readonly materielCode: string
  readonly materielDesignation: string
  readonly categorie: CategorieMateriel
  readonly chantierId: string
  readonly chantierNom: string
  readonly besoinId?: string | null
  readonly dateDebut: IsoDate
  readonly dateFinPrevue: IsoDate
  readonly dateFinReelle?: IsoDate | null
  readonly statut: StatutAffectation
  readonly dateReception?: IsoInstant | null
  readonly observationsReception?: string | null
  readonly fournisseurId?: string | null
  readonly contratId?: string | null
}

export interface Mouvement {
  readonly id: string
  readonly materielId: string
  readonly type: TypeMouvement
  readonly chantierOrigineNom?: string | null
  readonly chantierDestinationNom?: string | null
  readonly date: IsoInstant
  readonly auteurNom?: string | null
  readonly commentaire?: string | null
}

export interface Maintenance {
  readonly id: string
  readonly numeroOt: string
  readonly materielId: string
  readonly materielCode: string
  readonly materielDesignation: string
  readonly chantierId?: string | null
  readonly chantierNom?: string | null
  readonly type: TypeMaintenance
  readonly statut: StatutMaintenance
  readonly description: string
  readonly datePlanifiee: IsoDate
  readonly dateDebut?: IsoDate | null
  readonly dateFin?: IsoDate | null
  readonly technicien?: string | null
  readonly cout?: number | null
  readonly heuresCompteur?: number | null
  readonly observations?: string | null
  readonly panneId?: string | null
}

export interface Panne {
  readonly id: string
  readonly materielId: string
  readonly materielCode: string
  readonly materielDesignation: string
  readonly chantierId?: string | null
  readonly chantierNom?: string | null
  readonly gravite: GravitePanne
  readonly description: string
  readonly statut: StatutPanne
  readonly signaleParId: string
  readonly signaleParNom: string
  readonly dateSignalement: IsoInstant
  readonly maintenanceId?: string | null
}

/** Fiche complete : 20 derniers elements de chaque historique. */
export interface MaterielDetail extends Materiel {
  readonly affectations: readonly Affectation[]
  readonly maintenances: readonly Maintenance[]
  readonly pannes: readonly Panne[]
  readonly mouvements: readonly Mouvement[]
}

export interface TypeEngin {
  readonly id: string
  readonly code: string
  readonly libelle: string
  readonly categorie: CategorieMateriel
  readonly seuilMaintenanceHeures?: number | null
  readonly seuilMaintenanceJours?: number | null
  readonly actif: boolean
}

export interface Disponibilite {
  readonly disponible: boolean
  readonly quantiteDemandee: number
  readonly quantiteDisponible: number
  readonly materiels: readonly Materiel[]
}

/** Corps de POST /api/pannes. */
export interface PanneCreation {
  readonly materielId: string
  readonly gravite: GravitePanne
  readonly description: string
}

/** Proprietes d'une entite de GET /api/materiels/carte. */
export interface CarteFeatureProperties {
  readonly kind: 'CHANTIER' | 'MATERIEL'
  readonly id: string
  readonly code: string
  readonly libelle: string
  readonly categorie?: CategorieMateriel | null
  readonly etat?: EtatMateriel | null
  readonly statut?: StatutChantier | null
  /** Chantier d'affectation d'un materiel (contrat section 2 ; absent des anciennes versions de l'API). */
  readonly chantierId?: string | null
  readonly chantierNom?: string | null
  readonly nombreMateriels?: number | null
}

export interface CarteFeature {
  readonly type: 'Feature'
  /** GeoJSON : coordonnees [longitude, latitude]. */
  readonly geometry: { readonly type: 'Point'; readonly coordinates: readonly [number, number] }
  readonly properties: CarteFeatureProperties
}

export interface CarteFeatureCollection {
  readonly type: 'FeatureCollection'
  readonly features: readonly CarteFeature[]
}
