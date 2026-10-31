import { z } from 'zod'
import { ROLES } from '@/shared/domain/labels'
import { PASSWORD_RULES } from '../lib/password'

const montant = z
  .string()
  .trim()
  .regex(/^\d*$/, 'Montant entier en FCFA')
  .transform((value) => (value === '' ? undefined : Number(value)))

/** Mot de passe temporaire : regles de robustesse en francais. */
export const passwordSchema = z.string().superRefine((value, ctx) => {
  const failed = PASSWORD_RULES.find((rule) => !rule.test(value))
  if (failed) ctx.addIssue({ code: z.ZodIssueCode.custom, message: failed.message })
})

const base = z.object({
  prenom: z.string().trim().min(1, 'Prénom obligatoire').max(80),
  nom: z.string().trim().min(1, 'Nom obligatoire').max(80),
  email: z.string().trim().email('Adresse e-mail invalide'),
  telephone: z
    .string()
    .trim()
    .regex(/^(\+?\d[\d\s]{6,18})?$/, 'Numéro de téléphone invalide')
    .transform((value) => value || undefined),
  role: z.enum(ROLES, { errorMap: () => ({ message: 'Choisissez un rôle' }) }),
  chantierIds: z.array(z.string()),
  perimetreParc: z
    .string()
    .trim()
    .max(200)
    .transform((value) => value || undefined),
  seuilDelegation: montant,
  budgetGere: montant,
  motDePasseTemporaire: z.string(),
})

/**
 * Schema du formulaire utilisateur. Champs dependants du role :
 * CHEF -> au moins un chantier ; RA -> seuil de delegation ; DAF -> budget gere ; RM -> perimetre.
 * @param creation vrai a la creation (mot de passe temporaire obligatoire)
 */
export function utilisateurSchema(creation: boolean) {
  return base.superRefine((value, ctx) => {
    if (value.role === 'CHEF_CHANTIER' && value.chantierIds.length === 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['chantierIds'],
        message: 'Rattachez au moins un chantier',
      })
    }
    if (value.role === 'RESPONSABLE_ACHATS' && value.seuilDelegation === undefined) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['seuilDelegation'],
        message: 'Seuil de délégation obligatoire',
      })
    }
    if (creation) {
      const failed = PASSWORD_RULES.find((rule) => !rule.test(value.motDePasseTemporaire))
      if (failed)
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['motDePasseTemporaire'], message: failed.message })
    }
  })
}

export type UtilisateurFormInput = z.input<typeof base>
export type UtilisateurFormOutput = z.output<typeof base>
