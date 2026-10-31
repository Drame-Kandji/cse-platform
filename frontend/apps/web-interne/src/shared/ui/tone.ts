import type { Tone } from '@/shared/domain/labels'

/** Classes "douces" (fond teinte + texte accentue) par ton, conformes AA. */
export const softToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'bg-muted text-foreground/80 ring-border-strong/60',
  primary: 'bg-primary/10 text-primary-emphasis ring-primary/20',
  info: 'bg-info/10 text-info-emphasis ring-info/20',
  success: 'bg-success/10 text-success-emphasis ring-success/20',
  warning: 'bg-warning/10 text-warning-emphasis ring-warning/25',
  danger: 'bg-danger/10 text-danger-emphasis ring-danger/20',
  accent: 'bg-accent/15 text-accent-emphasis ring-accent/30',
}

/** Classes pleines par ton (fond plein + texte contraste). */
export const solidToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'bg-foreground text-background ring-transparent',
  primary: 'bg-primary text-primary-foreground ring-transparent',
  info: 'bg-info text-info-foreground ring-transparent',
  success: 'bg-success text-success-foreground ring-transparent',
  warning: 'bg-warning text-warning-foreground ring-transparent',
  danger: 'bg-danger text-danger-foreground ring-transparent',
  accent: 'bg-accent text-accent-foreground ring-transparent',
}

/** Couleur de pastille (point d'etat, timeline) par ton. */
export const dotToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'bg-muted-foreground',
  primary: 'bg-primary',
  info: 'bg-info',
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
  accent: 'bg-accent',
}

/** Couleur de texte accentuee (icones, valeurs) par ton. */
export const textToneClasses: Readonly<Record<Tone, string>> = {
  neutral: 'text-muted-foreground',
  primary: 'text-primary-emphasis',
  info: 'text-info-emphasis',
  success: 'text-success-emphasis',
  warning: 'text-warning-emphasis',
  danger: 'text-danger-emphasis',
  accent: 'text-accent-emphasis',
}
