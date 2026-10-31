import type { Tone } from '@/shared/domain/labels'

/*
 * Charte bleu + blanc : trois rendus seulement.
 * - neutre (gris discret)  : neutral, success (un etat "termine / ok" n'a pas besoin de couleur) ;
 * - bleu (actif, en cours) : primary, info, warning, accent ;
 * - rouge                  : danger (erreur, rejet, critique, en retard).
 */
const NEUTRAL_SOFT = 'bg-transparent text-muted-foreground ring-border-strong'
const BLUE_SOFT = 'bg-primary/[0.07] text-primary-emphasis ring-primary/20'
const RED_SOFT = 'bg-danger/[0.07] text-danger-emphasis ring-danger/25'

/** Classes "douces" (pastille discrete + texte accentue) par ton, conformes AA. */
export const softToneClasses: Readonly<Record<Tone, string>> = {
  neutral: NEUTRAL_SOFT,
  success: NEUTRAL_SOFT,
  primary: BLUE_SOFT,
  info: BLUE_SOFT,
  warning: BLUE_SOFT,
  accent: BLUE_SOFT,
  danger: RED_SOFT,
}

/** Classes pleines par ton (fond plein + texte contraste). */
export const solidToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'bg-foreground text-background ring-transparent',
  success: 'bg-foreground text-background ring-transparent',
  primary: 'bg-primary text-primary-foreground ring-transparent',
  info: 'bg-primary text-primary-foreground ring-transparent',
  warning: 'bg-primary text-primary-foreground ring-transparent',
  accent: 'bg-primary text-primary-foreground ring-transparent',
  danger: 'bg-danger text-danger-foreground ring-transparent',
}

/** Couleur de pastille (point d'etat, timeline) par ton. */
export const dotToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'bg-border-strong',
  success: 'bg-border-strong',
  primary: 'bg-primary',
  info: 'bg-primary',
  warning: 'bg-primary',
  accent: 'bg-primary',
  danger: 'bg-danger',
}

/** Couleur de texte accentuee (icones, valeurs) par ton. */
export const textToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'text-muted-foreground',
  success: 'text-muted-foreground',
  primary: 'text-primary-emphasis',
  info: 'text-primary-emphasis',
  warning: 'text-primary-emphasis',
  accent: 'text-primary-emphasis',
  danger: 'text-danger-emphasis',
}
