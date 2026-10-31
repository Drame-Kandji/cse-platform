import type { CategorieMateriel } from '@/shared/domain/labels'

/** Compte utilisateur (contrat §9) ; `id` = sub Keycloak. */
export interface Utilisateur {
  readonly id: string
  readonly email: string
  readonly prenom: string
  readonly nom: string
  readonly telephone?: string | null
  /** Role principal (code Keycloak, FOURNISSEUR inclus). */
  readonly role: string
  readonly actif: boolean
  readonly chantierIds?: readonly string[] | null
  readonly perimetreParc?: string | null
  readonly seuilDelegation?: number | null
  readonly budgetGere?: number | null
  readonly fournisseurId?: string | null
  readonly otpConfigure: boolean
  readonly createdAt: string
}

/** Corps de modification (PUT) : creation sans mot de passe. */
export interface UtilisateurModification {
  readonly email: string
  readonly prenom: string
  readonly nom: string
  readonly telephone?: string
  readonly role: string
  readonly chantierIds?: readonly string[]
  readonly perimetreParc?: string
  readonly seuilDelegation?: number
  readonly budgetGere?: number
}

/** Corps de creation (POST) : mot de passe temporaire a changer a la premiere connexion. */
export interface UtilisateurCreation extends UtilisateurModification {
  readonly motDePasseTemporaire: string
}

export interface UtilisateurFiltres {
  readonly role?: string
  readonly actif?: boolean
  readonly q?: string
}

export interface RoleInfo {
  readonly code: string
  readonly libelle: string
  readonly description: string
  readonly nombreUtilisateurs: number
  readonly otpObligatoire: boolean
}

/** Type d'engin du referentiel (seuils de maintenance preventive). */
export interface TypeEngin {
  readonly id: string
  readonly code: string
  readonly libelle: string
  readonly categorie: CategorieMateriel
  readonly seuilMaintenanceHeures?: number | null
  readonly seuilMaintenanceJours?: number | null
  readonly actif: boolean
}

export interface TypeEnginSaisie {
  readonly code: string
  readonly libelle: string
  readonly categorie: CategorieMateriel
  readonly seuilMaintenanceHeures?: number
  readonly seuilMaintenanceJours?: number
  readonly actif: boolean
}

/** Entree du journal d'audit (contrat §8). */
export interface AuditLog {
  readonly id: string
  readonly eventId: string
  readonly occurredAt: string
  readonly service: string
  readonly action: string
  readonly entite: string
  readonly entiteId: string
  readonly utilisateurId?: string | null
  readonly utilisateurNom?: string | null
  readonly roles: readonly string[]
  readonly resume?: string | null
  readonly correlationId?: string | null
}

export interface AuditFiltres {
  readonly utilisateurId?: string
  readonly entite?: string
  readonly action?: string
  readonly service?: string
  readonly du?: string
  readonly au?: string
  readonly q?: string
}

export interface AuditStatistiques {
  readonly total: number
  readonly aujourdhui: number
  readonly septDerniersJours: number
  readonly parService: readonly { service: string; total: number }[]
}

export type StatutService = 'UP' | 'DOWN'

/** Etat des services vu par la passerelle (`GET /api/plateforme/etat`). */
export interface EtatPlateforme {
  readonly services: readonly { nom: string; statut: StatutService; latenceMs?: number | null }[]
}
