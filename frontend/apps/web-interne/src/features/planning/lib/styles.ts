import type { StatutAffectation } from '@/shared/domain/labels'

/**
 * Couleur des barres par statut, charte bleu + blanc : bleu plein = en cours, bleu pale = en attente
 * de reception, gris = terminee (le libelle reste lisible au survol et par les lecteurs d'ecran).
 */
export const BAR_CLASSES: Readonly<Record<StatutAffectation, string>> = {
  EN_ATTENTE_RECEPTION: 'bg-primary/15 text-primary-emphasis hover:bg-primary/25',
  EN_COURS: 'bg-primary text-primary-foreground hover:bg-primary/90',
  TERMINEE: 'bg-muted-foreground/20 text-muted-foreground hover:bg-muted-foreground/30',
}
