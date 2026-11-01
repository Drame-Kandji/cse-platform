import { z } from 'zod'
import { entier, montantFcfa, texteFacultatif } from '@/domain/champs'

/** Depot d'une offre : montant FCFA, delai (jours), garantie (mois, facultative), description. */
export const offreSchema = z.object({
  montantPropose: montantFcfa('Montant'),
  delaiLivraisonJours: entier('Délai', 1),
  garantieMois: z
    .string()
    .trim()
    .refine((value) => value === '' || /^\d+$/.test(value), 'Garantie : nombre entier de mois')
    .transform((value) => (value === '' ? undefined : Number(value))),
  description: texteFacultatif(4000),
})
export type OffreInput = z.input<typeof offreSchema>
export type OffreOutput = z.output<typeof offreSchema>

export const OFFRE_VIDE: OffreInput = {
  montantPropose: '',
  delaiLivraisonJours: '',
  garantieMois: '',
  description: '',
}
