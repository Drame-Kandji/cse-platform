import { isBefore, startOfDay } from 'date-fns'
import { z } from 'zod'
import { GRAVITE_PANNE } from '@/shared/domain/labels'

const requiredDate = (message: string) =>
  z
    .date()
    .nullable()
    .refine((value): value is Date => value !== null, message)

/** Formulaire d'affectation d'un materiel a un chantier (R04). */
export const affectationSchema = z
  .object({
    materielId: z.string().min(1, 'Choisissez un matériel'),
    chantierId: z
      .string()
      .nullable()
      .refine((value) => Boolean(value), 'Choisissez un chantier'),
    besoinId: z.string().nullable(),
    dateDebut: requiredDate('Date de début obligatoire'),
    dateFinPrevue: requiredDate('Date de fin prévue obligatoire'),
  })
  .superRefine((value, ctx) => {
    if (isBefore(startOfDay(value.dateFinPrevue), startOfDay(value.dateDebut))) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['dateFinPrevue'],
        message: 'La fin prévue doit être postérieure ou égale au début',
      })
    }
  })

export type AffectationFormInput = z.input<typeof affectationSchema>
export type AffectationFormOutput = z.output<typeof affectationSchema>

/** Formulaire de signalement de panne. */
export const panneSchema = z.object({
  gravite: z.enum(GRAVITE_PANNE, { message: 'Choisissez une gravité' }),
  description: z
    .string()
    .trim()
    .min(10, 'Décrivez la panne en au moins 10 caractères')
    .max(2000, '2 000 caractères maximum'),
})

export type PanneFormInput = z.input<typeof panneSchema>
export type PanneFormOutput = z.output<typeof panneSchema>
