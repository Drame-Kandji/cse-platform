import { z } from 'zod'

/** Decision sur un engagement : commentaire obligatoire en cas de refus. */
export const decisionEngagementSchema = z
  .object({
    approuve: z.boolean(),
    commentaire: z.string().trim().max(500, '500 caractères maximum'),
  })
  .superRefine((value, ctx) => {
    if (!value.approuve && value.commentaire.length < 3) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['commentaire'],
        message: 'Le motif du refus est obligatoire (3 caractères minimum)',
      })
    }
  })
export type DecisionEngagementForm = z.infer<typeof decisionEngagementSchema>

/** Mandatement : reference du mandat de paiement. */
export const mandatSchema = z.object({
  referenceMandat: z
    .string()
    .trim()
    .min(1, 'Référence de mandat obligatoire')
    .max(50, '50 caractères maximum'),
})
export type MandatForm = z.infer<typeof mandatSchema>

/** Rejet d'une facture : motif obligatoire. */
export const rejetSchema = z.object({
  motif: z
    .string()
    .trim()
    .min(3, 'Motif obligatoire (3 caractères minimum)')
    .max(500, '500 caractères maximum'),
})
export type RejetForm = z.infer<typeof rejetSchema>

const montant = (label: string, allowZero: boolean) =>
  z
    .string()
    .trim()
    .min(1, `${label} obligatoire`)
    .regex(/^\d+$/, `${label} : nombre entier en FCFA`)
    .transform(Number)
    .pipe(allowZero ? z.number().nonnegative() : z.number().positive(`${label} doit être positif`))

const dateRequise = (message: string) =>
  z
    .date()
    .nullable()
    .refine((value): value is Date => value !== null, message)

/** Saisie d'une facture recue (montants entiers en FCFA). */
export const factureSchema = z
  .object({
    contratId: z.string().min(1, 'Contrat obligatoire'),
    numero: z.string().trim().min(1, 'Numéro de facture obligatoire').max(50, '50 caractères maximum'),
    montantHt: montant('Montant HT', false),
    tva: montant('TVA', true),
    dateEmission: dateRequise("Date d'émission obligatoire"),
    dateEcheance: dateRequise("Date d'échéance obligatoire"),
  })
  .refine((value) => value.dateEcheance >= value.dateEmission, {
    path: ['dateEcheance'],
    message: "L'échéance doit suivre la date d'émission",
  })
export type FactureFormInput = z.input<typeof factureSchema>
export type FactureFormOutput = z.output<typeof factureSchema>
