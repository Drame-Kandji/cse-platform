import { appelOffre, offre } from '@/test/fixtures'
import { decompte, estInvite, peutDeposer, resultatOffre } from './offres'

const NOW = Date.parse('2026-11-01T12:00:00Z')

describe('estInvite', () => {
  it('utilise le champ invite du contrat §13 quand il est present', () => {
    expect(estInvite({ type: 'APPEL_OFFRES', invite: true })).toBe(true)
    expect(estInvite({ type: 'DEMANDE_PRIX', invite: false })).toBe(false)
  })
  it('a defaut, une demande de prix implique une invitation', () => {
    expect(estInvite({ type: 'DEMANDE_PRIX' })).toBe(true)
    expect(estInvite({ type: 'APPEL_OFFRES' })).toBe(false)
  })
})

describe('resultatOffre', () => {
  it('reprend le resultat fourni par le backend', () => {
    expect(resultatOffre(offre({ resultat: 'RETENUE' }))).toBe('RETENUE')
  })
  it('deduit le resultat du statut de l appel d offres', () => {
    expect(resultatOffre(offre({ statutAppelOffre: 'PUBLIE' }))).toBe('EN_COURS')
    expect(resultatOffre(offre({ statutAppelOffre: 'CLOTURE', retenue: true }))).toBe('EN_COURS')
    expect(resultatOffre(offre({ statutAppelOffre: 'ATTRIBUE', retenue: true }))).toBe('RETENUE')
    expect(resultatOffre(offre({ statutAppelOffre: 'ATTRIBUE', retenue: false }))).toBe('NON_RETENUE')
  })
})

describe('decompte', () => {
  it('jours et heures au-dela de 24 h', () => {
    expect(decompte('2026-11-03T17:00:00Z', NOW)).toEqual({ label: '2 j 5 h', urgent: false, expire: false })
  })
  it('heures et minutes sous 24 h (urgent)', () => {
    expect(decompte('2026-11-01T15:05:00Z', NOW)).toEqual({ label: '3 h 05', urgent: true, expire: false })
  })
  it('minutes sous une heure', () => {
    expect(decompte('2026-11-01T12:00:20Z', NOW).label).toBe('1 min')
  })
  it('echeance depassee', () => {
    expect(decompte('2026-10-31T12:00:00Z', NOW)).toEqual({ label: 'Clôturé', urgent: false, expire: true })
  })
})

describe('peutDeposer', () => {
  it('publie, sans offre et avant la date limite', () => {
    expect(peutDeposer(appelOffre(), Date.now())).toBe(true)
    expect(peutDeposer(appelOffre({ offreDeposee: true }), Date.now())).toBe(false)
    expect(peutDeposer(appelOffre({ statut: 'CLOTURE' }), Date.now())).toBe(false)
    expect(peutDeposer(appelOffre({ dateLimite: '2020-01-01T00:00:00Z' }), Date.now())).toBe(false)
  })
})
