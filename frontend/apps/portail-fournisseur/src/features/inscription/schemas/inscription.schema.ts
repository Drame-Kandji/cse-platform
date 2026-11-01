import { z } from 'zod'
import { champsEntreprise, texteObligatoire } from '@/domain/champs'

/** Inscription publique d'un fournisseur (contrat §13, InscriptionRequest). */
export const inscriptionSchema = z.object({
  ...champsEntreprise,
  contactNom: texteObligatoire('Nom du contact', 150),
})
export type InscriptionInput = z.input<typeof inscriptionSchema>
export type InscriptionOutput = z.output<typeof inscriptionSchema>

export const INSCRIPTION_VIDE: InscriptionInput = {
  raisonSociale: '',
  ninea: '',
  rccm: '',
  adresse: '',
  ville: '',
  pays: 'Sénégal',
  telephone: '',
  email: '',
  contactNom: '',
  contactFonction: '',
  specialites: [],
}
