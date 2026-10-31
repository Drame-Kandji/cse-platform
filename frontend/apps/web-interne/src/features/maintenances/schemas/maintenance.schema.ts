import { z } from 'zod'
import { GRAVITE_PANNE, TYPE_MAINTENANCE } from '@/shared/domain/labels'

const description = z
  .string()
  .trim()
  .min(5, 'Décrivez en quelques mots (5 caractères au minimum)')
  .max(1000, '1000 caractères au maximum')

/** Planification d'un ordre de travail. */
export const planifierSchema = z.object({
  materielId: z.string().min(1, 'Choisissez un matériel'),
  type: z.enum(TYPE_MAINTENANCE),
  datePlanifiee: z
    .date()
    .nullable()
    .refine((date): date is Date => date !== null, 'Date prévue obligatoire'),
  technicien: z.string().trim().max(150, '150 caractères au maximum'),
  description,
})

/** Cloture : cout obligatoire (FCFA, entier positif ou nul), compteur facultatif. */
export const cloturerSchema = z.object({
  cout: z
    .string()
    .trim()
    .min(1, 'Coût obligatoire')
    .pipe(
      z.coerce
        .number({ invalid_type_error: 'Montant invalide' })
        .int('Montant entier en FCFA')
        .min(0, 'Le coût ne peut pas être négatif'),
    ),
  heuresCompteur: z
    .string()
    .trim()
    .transform((value) => (value === '' ? undefined : Number(value)))
    .refine((value) => value === undefined || (Number.isFinite(value) && value >= 0), 'Relevé invalide'),
  observations: z.string().trim().max(1000, '1000 caractères au maximum'),
})

/** Signalement d'une panne. */
export const panneSchema = z.object({
  materielId: z.string().min(1, 'Choisissez le matériel concerné'),
  gravite: z.enum(GRAVITE_PANNE),
  description,
})
