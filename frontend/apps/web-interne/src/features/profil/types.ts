/** Profil de l'utilisateur connecte (`Utilisateur`, contrat §9). */
export interface MonProfil {
  readonly id: string
  readonly email: string
  readonly prenom: string
  readonly nom: string
  readonly telephone?: string | null
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

/** Chantier (champs utiles a l'affichage du profil). */
export interface ChantierResume {
  readonly id: string
  readonly code: string
  readonly nom: string
  readonly region?: string
  readonly statut: string
}
