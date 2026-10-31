import { z } from 'zod'

const texteFacultatif = z
  .string()
  .trim()
  .max(2000, '2 000 caractères maximum')
  .transform((v) => (v === '' ? undefined : v))

/** Nombre facultatif saisi dans un champ texte ('' = absent). */
const nombreFacultatif = (max: number, message: string) =>
  z
    .union([z.literal(''), z.coerce.number().min(0, 'Valeur positive attendue').max(max, message)])
    .transform((v) => (v === '' ? undefined : v))

const dateObligatoire = z
  .date({ invalid_type_error: 'Date invalide' })
  .nullable()
  .refine((d): d is Date => d !== null, 'Date obligatoire')

/** Formulaire des clauses et jalons d'un contrat BROUILLON. */
export const clausesSchema = z.object({
  garantieMois: nombreFacultatif(240, '240 mois maximum'),
  conditionsMaintenance: texteFacultatif,
  conditionsLivraison: texteFacultatif,
  penaliteRetardPctParJour: nombreFacultatif(100, '100 % maximum'),
  delaiLivraisonJours: z.coerce
    .number({ invalid_type_error: 'Délai obligatoire' })
    .int('Nombre entier de jours')
    .min(1, 'Au moins 1 jour')
    .max(3650, '3 650 jours maximum'),
  jalons: z
    .array(
      z.object({
        libelle: z.string().trim().min(2, 'Au moins 2 caractères').max(200, '200 caractères maximum'),
        datePrevue: dateObligatoire,
      }),
    )
    .max(20, '20 jalons maximum'),
})

export type ClausesFormInput = z.input<typeof clausesSchema>
export type ClausesFormOutput = z.output<typeof clausesSchema>

/** Formulaire de confirmation de livraison. */
export const livraisonSchema = z.object({
  dateLivraisonReelle: dateObligatoire.refine(
    (d) => d.getTime() <= Date.now(),
    'La date ne peut pas être dans le futur',
  ),
  numeroBonLivraison: z.string().trim().min(2, 'Numéro du bon de livraison obligatoire').max(100),
  observations: texteFacultatif,
})

export type LivraisonFormInput = z.input<typeof livraisonSchema>
export type LivraisonFormOutput = z.output<typeof livraisonSchema>
