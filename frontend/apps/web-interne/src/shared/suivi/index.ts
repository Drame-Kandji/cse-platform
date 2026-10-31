/**
 * Briques ms-suivi partagees entre modules (parc, carte, planning, chantiers, maintenances) :
 * appels HTTP, cles de requete, hooks et dialogues metier reutilisables.
 */
export * from './api'
export * from './keys'
export * from './hooks'
export * from './schemas'
export { AffecterDialog, type AffecterDialogProps, type MaterielAffectable } from './AffecterDialog'
export { InlineAlert } from './AffecterDialogParts'
export { SignalerPanneDialog, type SignalerPanneDialogProps } from './SignalerPanneDialog'
