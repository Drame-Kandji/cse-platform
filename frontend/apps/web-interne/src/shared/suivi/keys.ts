import { createQueryKeys } from '@/shared/api/query-keys'

/** Cles du parc materiel (ms-suivi). Partagees par materiels, carte, planning et maintenances. */
export const materielKeys = createQueryKeys('materiels')
/** Cles des affectations (liste, planning). */
export const affectationKeys = createQueryKeys('affectations')
/** Cles des chantiers. */
export const chantierKeys = createQueryKeys('chantiers')
/** Cles des pannes. */
export const panneKeys = createQueryKeys('pannes')
/** Cles des maintenances. */
export const maintenanceKeys = createQueryKeys('maintenances')
/** Cles des besoins (options de rattachement d'une affectation). */
export const besoinOptionKeys = createQueryKeys('besoins')
