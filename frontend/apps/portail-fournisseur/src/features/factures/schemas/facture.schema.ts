import { z } from 'zod'
import { dateRequise, montantFcfa } from '@/domain/champs'

/** Taux de TVA en pourcentage (virgule ou point decimal). */
const taux = z
  .string()
  .trim()
  .regex(/^\d+([.,]\d+)?$/, 'Taux invalide')
  .transform((value) => Number(value.replace(',', '.')))
  .pipe(z.number().min(0).max(100, 'Taux entre 0 et 100'))

/** Nouvelle facture : contrat livre, numero, HT, taux de TVA (18 % par defaut), dates. */
export const factureSchema = z
  .object({
    contratId: z.string().min(1, 'Contrat obligatoire'),
    numero: z.string().trim().min(1, 'Numéro obligatoire').max(60, '60 caractères maximum'),
    montantHt: montantFcfa('Montant HT'),
    tauxTva: taux,
    dateEmission: dateRequise("Date d'émission obligatoire"),
    dateEcheance: dateRequise("Date d'échéance obligatoire"),
  })
  .refine((value) => value.dateEcheance >= value.dateEmission, {
    path: ['dateEcheance'],
    message: "L'échéance doit suivre la date d'émission",
  })
export type FactureInput = z.input<typeof factureSchema>
export type FactureOutput = z.output<typeof factureSchema>
