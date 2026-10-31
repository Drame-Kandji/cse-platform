import { z } from 'zod'
import {
  CATEGORIE_MATERIEL,
  CRITERE_EVALUATION,
  TYPE_APPEL_OFFRE,
  type CritereEvaluation,
  type TypeAppelOffre,
} from '@/shared/domain/labels'

/** Somme des poids des criteres (doit valoir 100). */
export function sommePoids(criteres: readonly { poids: number }[]): number {
  return criteres.reduce((total, critere) => total + (Number.isFinite(critere.poids) ? critere.poids : 0), 0)
}

/** Ponderation par defaut proposee a la creation (somme = 100). */
export const CRITERES_PAR_DEFAUT: { code: CritereEvaluation; poids: number }[] = [
  { code: 'PRIX', poids: 40 },
  { code: 'DELAI', poids: 20 },
  { code: 'QUALITE_TECHNIQUE', poids: 20 },
  { code: 'GARANTIE', poids: 10 },
  { code: 'SAV', poids: 5 },
  { code: 'REFERENCES', poids: 5 },
]

const nombre = (message: string) => z.number({ invalid_type_error: message, required_error: message })

export const criteresSchema = z
  .array(
    z.object({
      code: z.enum(CRITERE_EVALUATION),
      poids: nombre('Poids obligatoire')
        .int('Nombre entier attendu')
        .min(0, 'Minimum 0')
        .max(100, 'Maximum 100'),
    }),
  )
  .superRefine((criteres, ctx) => {
    const total = sommePoids(criteres)
    if (total !== 100) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `La somme des pondérations doit être égale à 100 (actuellement ${total}).`,
      })
    }
  })

export const appelOffreSchema = z
  .object({
    besoinId: z.string().optional(),
    objet: z.string().trim().min(5, 'Au moins 5 caractères').max(200, '200 caractères maximum'),
    description: z.string().trim().max(2000, '2000 caractères maximum').optional(),
    type: z.enum(TYPE_APPEL_OFFRE),
    categorie: z.enum(CATEGORIE_MATERIEL),
    quantite: nombre('Quantité obligatoire').int('Nombre entier attendu').min(1, 'Au moins 1'),
    specifications: z.string().trim().max(4000, '4000 caractères maximum').optional(),
    budgetEstime: z.number().int('Montant entier en FCFA').positive('Montant positif attendu').optional(),
    criteres: criteresSchema,
    fournisseurIdsInvites: z.array(z.string()),
    dateLimite: z
      .date({ invalid_type_error: 'Date limite obligatoire' })
      .nullable()
      .refine((date) => date !== null, 'Date limite obligatoire'),
    heureLimite: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'Heure au format HH:mm'),
  })
  .superRefine((value, ctx) => {
    const message = invitesManquants(value.type, value.fournisseurIdsInvites)
    if (message) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['fournisseurIdsInvites'], message })
    }
    if (Date.parse(instantLimite(value.dateLimite, value.heureLimite)) <= Date.now()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['dateLimite'],
        message: 'La date limite doit être dans le futur',
      })
    }
  })

export type AppelOffreFormInput = z.input<typeof appelOffreSchema>
export type AppelOffreFormOutput = z.output<typeof appelOffreSchema>

/**
 * Une demande de prix exige au moins un invite (INVITES_OBLIGATOIRES) ; un appel d'offres
 * reste ouvert a tout fournisseur actif de la specialite.
 * @returns message d'erreur, ou undefined si la selection est valide
 */
export function invitesManquants(type: TypeAppelOffre, invites: readonly string[]): string | undefined {
  return type === 'DEMANDE_PRIX' && invites.length === 0
    ? 'Une demande de prix exige au moins un fournisseur invité'
    : undefined
}

/** Combine date et heure locales en instant ISO-8601 UTC. */
export function instantLimite(date: Date, heure: string): string {
  const [h = '0', m = '0'] = heure.split(':')
  const instant = new Date(date)
  instant.setHours(Number(h), Number(m), 0, 0)
  return instant.toISOString()
}

/** Champs valides a chaque etape de l'assistant. */
export const CHAMPS_PAR_ETAPE = [
  ['objet', 'type', 'description'],
  ['categorie', 'quantite', 'specifications', 'budgetEstime'],
  ['criteres'],
  ['fournisseurIdsInvites'],
  ['dateLimite', 'heureLimite'],
  [],
] as const satisfies readonly (readonly (keyof AppelOffreFormInput)[])[]

/** Schema d'une offre saisie hors portail. */
export const offreSaisieSchema = z.object({
  fournisseurId: z.string({ required_error: 'Fournisseur obligatoire' }).min(1, 'Fournisseur obligatoire'),
  montantPropose: nombre('Montant obligatoire').int('Montant entier en FCFA').positive('Montant positif'),
  delaiLivraisonJours: nombre('Délai obligatoire').int('Nombre de jours entier').min(1, 'Au moins 1 jour'),
  garantieMois: z.number().int('Nombre de mois entier').min(0, 'Minimum 0').optional(),
  description: z.string().trim().max(2000, '2000 caractères maximum').optional(),
})
export type OffreSaisieInput = z.input<typeof offreSaisieSchema>

/** Note 0..100 facultative. */
const note = z.number().int('Note entière').min(0, 'Minimum 0').max(100, 'Maximum 100').optional()
export const evaluationOffreSchema = z.object({
  QUALITE_TECHNIQUE: note,
  GARANTIE: note,
  SAV: note,
  REFERENCES: note,
})
export type EvaluationOffreInput = z.input<typeof evaluationOffreSchema>

export const attributionSchema = z.object({
  offreId: z.string().min(1, 'Choisissez une offre'),
  justification: z.string().trim().min(20, 'Justification de 20 caractères minimum'),
})
export type AttributionInput = z.input<typeof attributionSchema>
