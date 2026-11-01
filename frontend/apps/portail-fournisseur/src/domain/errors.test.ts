import { ApiError } from '@/shared/api/errors'
import { estIntrouvable, MESSAGES_CODES, messageErreur } from './errors'

describe('messageErreur', () => {
  it.each([
    'DATE_LIMITE_DEPASSEE',
    'FOURNISSEUR_NON_ELIGIBLE',
    'TRANSITION_INTERDITE',
    'CONTRAT_NON_EN_LIVRAISON',
    'DATE_EXPEDITION_INVALIDE',
    'DATE_FUTURE',
    'ENGAGEMENT_NON_VALIDE',
    'CONTRAT_NON_LIVRE',
    'FOURNISSEUR_EXISTANT',
    'FACTURE_EXISTANTE',
  ])('traduit le code %s', (code) => {
    const message = messageErreur(new ApiError(422, { code, detail: 'detail serveur' }))
    expect(message).toBe(MESSAGES_CODES[code])
    expect(message).not.toMatch(/[\u2013\u2014]/)
  })
  it('contextualise un 409 sans code connu', () => {
    const error = new ApiError(409, { detail: 'Conflict' })
    expect(messageErreur(error, { conflit: 'Offre déjà déposée' })).toBe('Offre déjà déposée')
    expect(messageErreur(error)).toBe('Conflict')
  })
  it('retombe sur le message du serveur ou par defaut', () => {
    expect(messageErreur(new ApiError(500, { detail: 'Panne' }))).toBe('Panne')
    expect(messageErreur(new Error('Oups'))).toBe('Oups')
  })
  it('estIntrouvable', () => {
    expect(estIntrouvable(new ApiError(404))).toBe(true)
    expect(estIntrouvable(new ApiError(403))).toBe(false)
    expect(estIntrouvable(new Error('x'))).toBe(false)
  })
})
