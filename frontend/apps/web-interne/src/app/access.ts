import type { Role } from '@/shared/domain/labels'

const ALL: readonly Role[] = []

/**
 * Matrice d'acces des ecrans (ergonomie uniquement : l'API applique @PreAuthorize).
 * Partagee par le routeur (RequireRole), la navigation et la palette de commandes.
 * Liste vide = tout utilisateur authentifie.
 */
export const access = {
  dashboard: ALL,
  assistant: ALL,
  notifications: ALL,
  profil: ALL,
  /* Besoins (contrat §3) : lecture CHEF/RM/RA/DG, creation CHEF/RM, validation RM seul (R01). */
  besoins: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'RESPONSABLE_ACHATS', 'DIRECTION_GENERALE'],
  besoinNouveau: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL'],
  validation: ['RESPONSABLE_MATERIEL'],
  materiels: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'RESPONSABLE_ACHATS', 'DIRECTION_GENERALE'],
  carte: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE'],
  planning: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE'],
  /* Chantiers & maintenance (contrat §2) : lecture tous internes ; gestion RM (+ ADMIN pour les chantiers). */
  chantiers: ALL,
  chantierGestion: ['RESPONSABLE_MATERIEL', 'ADMIN_SYSTEME'],
  chantierTerminer: ['RESPONSABLE_MATERIEL'],
  maintenances: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'RESPONSABLE_ACHATS', 'DIRECTION_GENERALE'],
  maintenanceGestion: ['RESPONSABLE_MATERIEL'],
  panneSignalement: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL'],
  appelsOffres: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'DAF', 'DIRECTION_GENERALE'],
  contrats: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'DAF', 'DIRECTION_GENERALE'],
  fournisseurs: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'DAF', 'DIRECTION_GENERALE'],
  finance: ['DAF', 'DIRECTION_GENERALE'],
  /* Actions achats / finance (contrat §4-§5). */
  achatsGestion: ['RESPONSABLE_ACHATS'],
  attributionCoValidation: ['RESPONSABLE_MATERIEL'],
  attributionApprobation: ['DIRECTION_GENERALE'],
  offresLecture: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE'],
  offresEvaluation: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL'],
  besoinsAAcheter: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL'],
  fournisseurEvaluation: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL'],
  litigeOuverture: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL'],
  litigesLecture: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE'],
  blacklistageDecision: ['DIRECTION_GENERALE'],
  financeGestion: ['DAF'],
  engagementsLecture: ['DAF', 'RESPONSABLE_ACHATS', 'DIRECTION_GENERALE'],
  facturesLecture: ['DAF', 'RESPONSABLE_ACHATS'],
  factureSaisie: ['RESPONSABLE_ACHATS', 'DAF'],
  /* Reporting (contrat §6) : chaque profil metier a son tableau ; CHEF filtre sur ses chantiers. */
  rapports: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'RESPONSABLE_ACHATS', 'DAF', 'DIRECTION_GENERALE'],
  admin: ['ADMIN_SYSTEME'],
} as const satisfies Record<string, readonly Role[]>

export type AccessKey = keyof typeof access
