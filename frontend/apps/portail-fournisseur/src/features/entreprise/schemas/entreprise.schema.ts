import { z } from 'zod'
import { CATEGORIE_MATERIEL } from '@/shared/domain/labels'
import { champsEntreprise, entier, montantFcfa, texteFacultatif, texteObligatoire } from '@/domain/champs'

const IBAN_REGEX = /^[A-Za-z]{2}[0-9]{2}[A-Za-z0-9 ]{11,40}$/

/** Coordonnees modifiables par le fournisseur (ModifierFournisseurRequest, sans `version`). */
export const profilSchema = z
  .object({
    ...champsEntreprise,
    contactNom: texteFacultatif(150),
    /** Une certification par ligne. */
    certifications: z.string().transform((value) =>
      value
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean),
    ),
    banque: z.string().trim().max(150, '150 caractères maximum'),
    iban: z.string().trim(),
  })
  .superRefine((value, ctx) => {
    if (value.banque === '' && value.iban === '') return
    if (value.banque === '')
      ctx.addIssue({ code: 'custom', path: ['banque'], message: 'Banque obligatoire avec un IBAN' })
    if (!IBAN_REGEX.test(value.iban))
      ctx.addIssue({ code: 'custom', path: ['iban'], message: 'IBAN invalide' })
  })
export type ProfilInput = z.input<typeof profilSchema>
export type ProfilOutput = z.output<typeof profilSchema>

/** Ligne de catalogue (LigneCatalogueRequest, upsert par reference). */
export const ligneSchema = z.object({
  reference: texteObligatoire('Référence', 80),
  designation: texteObligatoire('Désignation', 255),
  categorie: z.enum(CATEGORIE_MATERIEL, { errorMap: () => ({ message: 'Catégorie obligatoire' }) }),
  prixUnitaire: montantFcfa('Prix unitaire'),
  unite: texteObligatoire('Unité', 30),
  delaiLivraisonJours: entier('Délai', 0),
})
export type LigneInput = z.input<typeof ligneSchema>
export type LigneOutput = z.output<typeof ligneSchema>
