import { z } from 'zod'

/** Note d'un critere d'evaluation, bornee de 0 a 10. */
const note = z
  .number({ required_error: 'Note obligatoire', invalid_type_error: 'Note obligatoire' })
  .int('La note doit être un nombre entier')
  .min(0, 'La note doit être comprise entre 0 et 10')
  .max(10, 'La note doit être comprise entre 0 et 10')

/** Criteres d'evaluation d'un fournisseur (contrat §4, POST /evaluer). */
export const CRITERES_EVALUATION = [
  { name: 'qualite', label: 'Qualité' },
  { name: 'delais', label: 'Respect des délais' },
  { name: 'conformite', label: 'Conformité' },
  { name: 'sav', label: 'Service après-vente' },
  { name: 'prix', label: 'Prix' },
] as const

export const evaluationSchema = z.object({
  qualite: note,
  delais: note,
  conformite: note,
  sav: note,
  prix: note,
  commentaire: z.string().trim().max(2000, '2000 caractères maximum').optional(),
})

export type EvaluationForm = z.infer<typeof evaluationSchema>
