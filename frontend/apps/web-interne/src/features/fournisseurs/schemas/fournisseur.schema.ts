import { z } from 'zod'
import { CATEGORIE_MATERIEL, GRAVITE_LITIGE, TYPE_DOCUMENT } from '@/shared/domain/labels'

const optionalText = z
  .string()
  .trim()
  .max(255, '255 caractères maximum')
  .transform((value) => (value === '' ? undefined : value))
  .optional()

/** Formulaire de creation / modification d'un fournisseur (RA). */
export const fournisseurSchema = z
  .object({
    raisonSociale: z.string().trim().min(2, 'Raison sociale obligatoire').max(200, '200 caractères maximum'),
    ninea: z
      .string()
      .trim()
      .regex(/^[0-9]{7,9} ?[0-9A-Za-z]{3}$/, 'NINEA invalide (7 à 9 chiffres puis un code de 3 caractères)'),
    rccm: optionalText,
    adresse: optionalText,
    ville: optionalText,
    pays: z.string().trim().min(2, 'Pays obligatoire'),
    telephone: z
      .string()
      .trim()
      .refine((value) => value === '' || /^\+?[0-9 ]{7,20}$/.test(value), 'Numéro de téléphone invalide')
      .transform((value) => (value === '' ? undefined : value))
      .optional(),
    email: z.string().trim().email('Adresse e-mail invalide').max(200, '200 caractères maximum'),
    contactNom: optionalText,
    contactFonction: optionalText,
    specialites: z.array(z.enum(CATEGORIE_MATERIEL)).min(1, 'Choisissez au moins une spécialité'),
    certifications: z.string().trim(),
    banque: optionalText,
    iban: z
      .string()
      .trim()
      .refine(
        (value) => value === '' || /^[A-Za-z]{2}[0-9]{2}[A-Za-z0-9 ]{11,40}$/.test(value),
        'IBAN invalide',
      )
      .transform((value) => (value === '' ? undefined : value))
      .optional(),
  })
  .refine((value) => Boolean(value.banque) === Boolean(value.iban), {
    message: 'Renseignez la banque et l’IBAN ensemble',
    path: ['iban'],
  })

export type FournisseurFormInput = z.input<typeof fournisseurSchema>
export type FournisseurFormOutput = z.output<typeof fournisseurSchema>

/** Ajout d'un document administratif. */
export const documentSchema = z.object({
  type: z.enum(TYPE_DOCUMENT, { required_error: 'Choisissez un type de document' }),
  libelle: z.string().trim().min(2, 'Libellé obligatoire').max(200, '200 caractères maximum'),
  dateExpiration: z.date().nullable(),
})
export type DocumentForm = z.infer<typeof documentSchema>

/** Ouverture d'un litige. */
export const litigeSchema = z.object({
  objet: z
    .string()
    .trim()
    .min(3, 'Objet obligatoire (au moins 3 caractères)')
    .max(255, '255 caractères maximum'),
  description: z
    .string()
    .trim()
    .min(10, 'Décrivez le litige (au moins 10 caractères)')
    .max(4000, '4000 caractères maximum'),
  gravite: z.enum(GRAVITE_LITIGE, { required_error: 'Choisissez une gravité' }),
})
export type LitigeForm = z.infer<typeof litigeSchema>

/** Ligne de catalogue (upsert par reference). */
export const ligneCatalogueSchema = z.object({
  reference: z.string().trim().min(1, 'Référence obligatoire').max(80, '80 caractères maximum'),
  designation: z.string().trim().min(2, 'Désignation obligatoire'),
  categorie: z.enum(CATEGORIE_MATERIEL, { required_error: 'Choisissez une catégorie' }),
  prixUnitaire: z.coerce.number({ invalid_type_error: 'Montant attendu' }).int().positive('Prix positif'),
  unite: z.string().trim().min(1, 'Unité obligatoire').max(30, '30 caractères maximum'),
  delaiLivraisonJours: z.coerce.number({ invalid_type_error: 'Nombre attendu' }).int().min(0, 'Délai ≥ 0'),
})
export type LigneCatalogueFormInput = z.input<typeof ligneCatalogueSchema>
export type LigneCatalogueFormOutput = z.output<typeof ligneCatalogueSchema>

/** Motif obligatoire (suspension, demande de blacklistage). */
export const motifSchema = z
  .string()
  .trim()
  .min(5, 'Motif obligatoire (au moins 5 caractères)')
  .max(1000, '1000 caractères maximum')
