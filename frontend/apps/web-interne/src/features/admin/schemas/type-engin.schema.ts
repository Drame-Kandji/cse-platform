import { z } from 'zod'
import { CATEGORIE_MATERIEL } from '@/shared/domain/labels'

const seuil = z
  .string()
  .trim()
  .regex(/^\d*$/, 'Nombre entier positif')
  .transform((value) => (value === '' ? undefined : Number(value)))
  .refine((value) => value === undefined || value > 0, 'Doit être supérieur à 0')

/** Type d'engin : code unique, libelle, categorie et seuils de maintenance preventive. */
export const typeEnginSchema = z.object({
  code: z
    .string()
    .trim()
    .min(2, 'Au moins 2 caractères')
    .max(30)
    .regex(/^[A-Za-z0-9_-]+$/, 'Lettres, chiffres, tirets uniquement')
    .transform((value) => value.toUpperCase()),
  libelle: z.string().trim().min(2, 'Au moins 2 caractères').max(120),
  categorie: z.enum(CATEGORIE_MATERIEL, { errorMap: () => ({ message: 'Choisissez une catégorie' }) }),
  seuilMaintenanceHeures: seuil,
  seuilMaintenanceJours: seuil,
  actif: z.boolean(),
})

export type TypeEnginFormInput = z.input<typeof typeEnginSchema>
export type TypeEnginFormOutput = z.output<typeof typeEnginSchema>
