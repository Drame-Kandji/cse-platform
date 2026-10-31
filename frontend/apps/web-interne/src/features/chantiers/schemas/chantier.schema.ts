import { z } from 'zod'
import { toIsoDate } from '@/shared/lib/format'
import type { Chantier, ChantierPayload } from '../api/types'

/** Regions administratives du Senegal (liste de saisie). */
export const REGIONS = [
  'Dakar',
  'Diourbel',
  'Fatick',
  'Kaffrine',
  'Kaolack',
  'Kédougou',
  'Kolda',
  'Louga',
  'Matam',
  'Saint-Louis',
  'Sédhiou',
  'Tambacounda',
  'Thiès',
  'Ziguinchor',
] as const

const montant = (label: string) =>
  z.coerce
    .number({ invalid_type_error: `${label} invalide` })
    .int('Montant entier en FCFA attendu')
    .min(0, 'Le montant ne peut pas être négatif')

const coordonnee = (min: number, max: number) =>
  z.coerce
    .number({ invalid_type_error: 'Coordonnée invalide' })
    .min(min, `Entre ${min} et ${max}`)
    .max(max, `Entre ${min} et ${max}`)

/** Formulaire de creation / modification d'un chantier. */
export const chantierSchema = z
  .object({
    code: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z0-9-]{3,20}$/, 'Code de 3 à 20 caractères (lettres, chiffres, tirets)'),
    nom: z.string().trim().min(3, 'Au moins 3 caractères').max(150, '150 caractères au maximum'),
    localisation: z.string().trim().min(2, 'Localisation obligatoire').max(150, '150 caractères au maximum'),
    region: z.string().min(1, 'Choisissez une région'),
    latitude: coordonnee(-90, 90),
    longitude: coordonnee(-180, 180),
    budget: montant('Budget'),
    montantMarche: montant('Montant'),
    dateDebut: z
      .date()
      .nullable()
      .refine((date): date is Date => date !== null, 'Date de début obligatoire'),
    dateFinPrevue: z.date().nullable(),
    chefChantierId: z.string().trim(),
    chefChantierNom: z.string().trim().max(150, '150 caractères au maximum'),
  })
  .superRefine((values, ctx) => {
    if (values.dateFinPrevue && values.dateFinPrevue < values.dateDebut) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['dateFinPrevue'],
        message: 'La fin prévue doit suivre la date de début',
      })
    }
  })

export type ChantierFormInput = z.input<typeof chantierSchema>
export type ChantierFormValues = z.output<typeof chantierSchema>

const parseDate = (value: string | null | undefined) => (value ? new Date(`${value}T00:00:00`) : null)

/** Valeurs initiales du formulaire (vide ou chantier existant). */
export function chantierDefaults(chantier?: Chantier | null): ChantierFormInput {
  return {
    code: chantier?.code ?? '',
    nom: chantier?.nom ?? '',
    localisation: chantier?.localisation ?? '',
    region: chantier?.region ?? '',
    latitude: chantier?.latitude ?? 14.6928,
    longitude: chantier?.longitude ?? -17.4467,
    budget: chantier?.budget ?? 0,
    montantMarche: chantier?.montantMarche ?? 0,
    dateDebut: parseDate(chantier?.dateDebut),
    dateFinPrevue: parseDate(chantier?.dateFinPrevue),
    chefChantierId: chantier?.chefChantierId ?? '',
    chefChantierNom: chantier?.chefChantierNom ?? '',
  }
}

/** Valeurs du formulaire -> corps d'API (champs facultatifs vides omis). */
export function toChantierPayload(values: ChantierFormValues): ChantierPayload {
  return {
    code: values.code,
    nom: values.nom,
    localisation: values.localisation,
    region: values.region,
    latitude: values.latitude,
    longitude: values.longitude,
    budget: values.budget,
    montantMarche: values.montantMarche,
    dateDebut: toIsoDate(values.dateDebut) ?? '',
    dateFinPrevue: toIsoDate(values.dateFinPrevue),
    chefChantierId: values.chefChantierId || undefined,
    chefChantierNom: values.chefChantierNom || undefined,
  }
}
