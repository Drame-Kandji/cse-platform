import { z } from 'zod'
import { CATEGORIE_MATERIEL } from '@/shared/domain/labels'

/**
 * Briques zod communes aux formulaires du portail (memes contraintes que les DTO Java :
 * InscriptionRequest, ModifierFournisseurRequest). Messages en francais.
 */

/** Texte facultatif : vide => undefined (champ omis dans le JSON). */
export const texteFacultatif = (max: number) =>
  z
    .string()
    .trim()
    .max(max, `${max} caractères maximum`)
    .transform((value) => value || undefined)

export const texteObligatoire = (label: string, max: number) =>
  z.string().trim().min(1, `${label} obligatoire`).max(max, `${max} caractères maximum`)

export const NINEA_REGEX = /^[0-9]{7,9} ?[0-9A-Za-z]{3}$/
export const TELEPHONE_REGEX = /^\+?[0-9 ]{7,20}$/

export const champsEntreprise = {
  raisonSociale: texteObligatoire('Raison sociale', 200),
  ninea: z.string().trim().regex(NINEA_REGEX, 'NINEA invalide (ex. 005123456 2G3)'),
  rccm: texteFacultatif(50),
  adresse: texteFacultatif(255),
  ville: texteFacultatif(100),
  pays: texteObligatoire('Pays', 100),
  telephone: z
    .string()
    .trim()
    .refine((value) => value === '' || TELEPHONE_REGEX.test(value), 'Numéro de téléphone invalide')
    .transform((value) => value || undefined),
  email: z.string().trim().min(1, 'E-mail obligatoire').email('Adresse e-mail invalide').max(200),
  contactFonction: texteFacultatif(150),
  specialites: z.array(z.enum(CATEGORIE_MATERIEL)).min(1, 'Choisissez au moins une spécialité'),
}

/** Montant entier en FCFA saisi en texte (espaces de milliers toleres). */
export const montantFcfa = (label: string) =>
  z
    .string()
    .transform((value) => value.replace(/[\s\u00a0\u202f]/g, ''))
    .pipe(
      z
        .string()
        .min(1, `${label} obligatoire`)
        .regex(/^\d+$/, `${label} : nombre entier en FCFA`)
        .transform(Number)
        .pipe(z.number().positive(`${label} doit être positif`)),
    )

/** Entier saisi en texte, borne inferieure `min`. */
export const entier = (label: string, min: number) =>
  z
    .string()
    .trim()
    .min(1, `${label} obligatoire`)
    .regex(/^\d+$/, `${label} : nombre entier`)
    .transform(Number)
    .pipe(z.number().min(min, `${label} : minimum ${min}`))

/** Date obligatoire (DatePicker : Date | null). */
export const dateRequise = (message: string) =>
  z
    .date()
    .nullable()
    .refine((value): value is Date => value !== null, message)
