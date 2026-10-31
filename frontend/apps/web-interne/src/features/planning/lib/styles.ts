import type { StatutAffectation } from '@/shared/domain/labels'

/** Couleur des barres par statut (le libelle reste lisible au survol et par les lecteurs d'ecran). */
export const BAR_CLASSES: Readonly<Record<StatutAffectation, string>> = {
  EN_ATTENTE_RECEPTION:
    'border border-dashed border-warning bg-warning/15 text-warning-emphasis hover:bg-warning/25',
  EN_COURS: 'bg-primary text-primary-foreground hover:bg-primary/90',
  TERMINEE: 'bg-muted-foreground/25 text-foreground/80 hover:bg-muted-foreground/35',
}
