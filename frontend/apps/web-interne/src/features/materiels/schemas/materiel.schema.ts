import { z } from 'zod'
import { CATEGORIE_MATERIEL } from '@/shared/domain/labels'

const optionalText = (max: number) =>
  z
    .string()
    .trim()
    .max(max, `${String(max)} caractères maximum`)
    .transform((value) => value || undefined)

/** Montant ou compteur saisi en texte ("1 250 000" accepte) -> entier positif. */
const integerText = (label: string, required: boolean) =>
  z
    .string()
    .transform((value) => value.replace(/\s/g, ''))
    .superRefine((value, ctx) => {
      if (value === '') {
        if (required) ctx.addIssue({ code: z.ZodIssueCode.custom, message: `${label} obligatoire` })
        return
      }
      if (!/^\d+$/.test(value)) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: `${label} : nombre entier positif attendu` })
      }
    })
    .transform((value) => (value === '' ? undefined : Number(value)))

/** Formulaire de creation / modification d'un materiel (RM). */
export const materielSchema = z.object({
  code: z
    .string()
    .trim()
    .min(2, 'Code obligatoire')
    .max(40, '40 caractères maximum')
    .regex(/^[A-Za-z0-9-_/]+$/, 'Lettres, chiffres et tirets uniquement'),
  immatriculation: optionalText(30),
  designation: z.string().trim().min(3, 'Au moins 3 caractères').max(200, '200 caractères maximum'),
  categorie: z.enum(CATEGORIE_MATERIEL, { message: 'Choisissez une catégorie' }),
  typeEnginId: z
    .string()
    .nullable()
    .transform((value) => value ?? undefined),
  marque: optionalText(80),
  modele: optionalText(80),
  tarifJournalier: integerText('Tarif journalier', true),
  valeurAcquisition: integerText("Valeur d'acquisition", false),
  dateAcquisition: z.date().nullable(),
  heuresFonctionnement: integerText('Compteur horaire', false),
})

export type MaterielFormInput = z.input<typeof materielSchema>
export type MaterielFormOutput = z.output<typeof materielSchema>
