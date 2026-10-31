import { startOfDay } from 'date-fns'
import { z } from 'zod'
import { CATEGORIE_MATERIEL, PRIORITE, type CategorieMateriel, type Priorite } from '@/shared/domain/labels'
import { toIsoDate } from '@/shared/lib/format'
import type { Besoin, BesoinPayload } from '../api/types'

/** R08 : une priorite URGENT ou CRITIQUE exige une justification. */
export function justificationRequise(priorite: Priorite | undefined): boolean {
  return priorite === 'URGENT' || priorite === 'CRITIQUE'
}

const JUSTIFICATION_MIN = 10

const champsBesoin = z.object({
  chantierId: z.string().min(1, 'Choisissez un chantier'),
  categorie: z
    .enum(CATEGORIE_MATERIEL)
    .nullable()
    .refine((value): value is CategorieMateriel => value !== null, 'Choisissez une catégorie'),
  designation: z.string().trim().min(3, 'Au moins 3 caractères').max(200, '200 caractères au maximum'),
  quantite: z.coerce
    .number({ invalid_type_error: 'Quantité invalide' })
    .int('Nombre entier attendu')
    .min(1, 'Au moins 1')
    .max(999, '999 au maximum'),
  dureeUtilisationJours: z.coerce
    .number({ invalid_type_error: 'Durée invalide' })
    .int('Nombre entier de jours attendu')
    .min(1, 'Au moins 1 jour')
    .max(3650, '3650 jours au maximum'),
  dateBesoin: z
    .date()
    .nullable()
    .refine((date): date is Date => date !== null, 'Date souhaitée obligatoire')
    .refine((date) => date >= startOfDay(new Date()), 'La date ne peut pas être passée'),
  priorite: z.enum(PRIORITE),
  justification: z.string().trim().max(1000, '1000 caractères au maximum'),
  specifications: z.string().trim().max(2000, '2000 caractères au maximum'),
})

/** R08, evalue independamment des autres champs (un superRefine d'objet serait ignore si un champ echoue). */
const regleJustification = z
  .object({ priorite: z.enum(PRIORITE), justification: z.string().trim() })
  .superRefine((values, ctx) => {
    if (justificationRequise(values.priorite) && values.justification.length < JUSTIFICATION_MIN) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['justification'],
        message:
          values.justification.length === 0
            ? 'Justification obligatoire pour une priorité urgente ou critique'
            : `Au moins ${JUSTIFICATION_MIN} caractères`,
      })
    }
  })

/** Formulaire d'expression (creation et modification) d'un besoin. */
export const besoinSchema = z.intersection(champsBesoin, regleJustification)

export type BesoinFormInput = z.input<typeof besoinSchema>
export type BesoinFormValues = z.output<typeof besoinSchema>

/** Valeurs initiales : nouveau besoin (chantier pre-rempli si unique) ou edition. */
export function besoinDefaults(besoin?: Besoin, chantierId = ''): BesoinFormInput {
  if (!besoin) {
    return {
      chantierId,
      categorie: null,
      designation: '',
      quantite: 1,
      dureeUtilisationJours: 30,
      dateBesoin: null,
      priorite: 'NORMAL',
      justification: '',
      specifications: '',
    }
  }
  return {
    chantierId: besoin.chantierId,
    categorie: besoin.categorie,
    designation: besoin.designation,
    quantite: besoin.quantite,
    dureeUtilisationJours: besoin.dureeUtilisationJours,
    dateBesoin: new Date(`${besoin.dateBesoin}T00:00:00`),
    priorite: besoin.priorite,
    justification: besoin.justification ?? '',
    specifications: besoin.specifications ?? '',
  }
}

/** Convertit les valeurs du formulaire en corps d'API (champs vides omis, justification si R08). */
export function toBesoinPayload(values: BesoinFormValues): BesoinPayload {
  return {
    chantierId: values.chantierId,
    categorie: values.categorie,
    designation: values.designation,
    quantite: values.quantite,
    dureeUtilisationJours: values.dureeUtilisationJours,
    dateBesoin: toIsoDate(values.dateBesoin) ?? '',
    priorite: values.priorite,
    justification: values.justification || undefined,
    specifications: values.specifications || undefined,
  }
}

/** Motif de rejet obligatoire. */
export const rejetSchema = z.object({
  motif: z
    .string()
    .trim()
    .min(5, 'Motif obligatoire (5 caractères au minimum)')
    .max(500, '500 caractères au maximum'),
})
