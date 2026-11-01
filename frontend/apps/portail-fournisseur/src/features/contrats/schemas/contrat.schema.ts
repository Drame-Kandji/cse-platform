import { z } from 'zod'
import { dateRequise, texteFacultatif } from '@/domain/champs'

/** Refus de signature : motif obligatoire. */
export const refusSchema = z.object({
  motif: z
    .string()
    .trim()
    .min(3, 'Motif obligatoire (3 caractères minimum)')
    .max(500, '500 caractères maximum'),
})
export type RefusForm = z.infer<typeof refusSchema>

// Les raffinements zod s'enchainent meme en echec : la date peut encore etre nulle ici.
const pasDansLeFutur = (date: Date | null) => date === null || date.getTime() <= Date.now()

/** Declaration d'expedition : date (pas dans le futur) et commentaire facultatif. */
export const expeditionSchema = z.object({
  dateExpedition: dateRequise("Date d'expédition obligatoire").refine(
    pasDansLeFutur,
    'La date ne peut pas être future',
  ),
  commentaire: texteFacultatif(500),
})
export type ExpeditionInput = z.input<typeof expeditionSchema>
export type ExpeditionOutput = z.output<typeof expeditionSchema>

/** Jalon atteint : date reelle (pas dans le futur). */
export const jalonSchema = z.object({
  dateReelle: dateRequise('Date obligatoire').refine(pasDansLeFutur, 'La date ne peut pas être future'),
})
export type JalonInput = z.input<typeof jalonSchema>
export type JalonOutput = z.output<typeof jalonSchema>
