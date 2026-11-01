import { contrat, facture } from '@/test/fixtures'
import { actionsContrat, contratsActifs, contratsOnglet, ETAPES_CONTRAT, etapeCourante } from './contrats'
import { STATUT_CONTRAT_PORTAIL, type StatutContrat } from './types'

describe('actionsContrat (statut -> actions du fournisseur)', () => {
  const attendu: Record<StatutContrat, string[]> = {
    BROUILLON: [],
    EN_ATTENTE_SIGNATURE: ['SIGNER', 'REFUSER'],
    SIGNE: [],
    ENGAGEMENT_VALIDE: ['EXPEDIER'],
    EN_LIVRAISON: ['JALON'],
    LIVRE: ['FACTURER'],
    CLOTURE: [],
  }
  it.each(STATUT_CONTRAT_PORTAIL)('%s', (statut) => {
    expect(actionsContrat(contrat({ statut }), [])).toEqual(attendu[statut])
  })
  it('en livraison sans jalon restant : aucune action', () => {
    const c = contrat({
      statut: 'EN_LIVRAISON',
      jalons: [
        { id: 'j', libelle: 'L', datePrevue: '2026-12-01', dateReelle: '2026-11-30', statut: 'ATTEINT' },
      ],
    })
    expect(actionsContrat(c, [])).toEqual([])
  })
  it('livre deja facture : plus de depot, sauf si la facture a ete rejetee', () => {
    const c = contrat({ statut: 'LIVRE' })
    expect(actionsContrat(c, [facture({ contratId: c.id })])).toEqual([])
    expect(actionsContrat(c, [facture({ contratId: c.id, statut: 'REJETEE' })])).toEqual(['FACTURER'])
  })
})

describe('etapeCourante', () => {
  it('suit le cycle Signature -> Cloture', () => {
    expect(etapeCourante('BROUILLON', false)).toBe(0)
    expect(etapeCourante('EN_ATTENTE_SIGNATURE', false)).toBe(0)
    expect(etapeCourante('SIGNE', false)).toBe(1)
    expect(etapeCourante('ENGAGEMENT_VALIDE', false)).toBe(2)
    expect(etapeCourante('EN_LIVRAISON', false)).toBe(3)
    expect(etapeCourante('LIVRE', false)).toBe(4)
    expect(etapeCourante('LIVRE', true)).toBe(5)
    expect(etapeCourante('CLOTURE', true)).toBe(ETAPES_CONTRAT.length)
  })
})

describe('onglets et KPI', () => {
  const liste = STATUT_CONTRAT_PORTAIL.map((statut) => contrat({ id: statut, statut }))
  it('repartit les contrats par onglet', () => {
    expect(contratsOnglet(liste, 'A_SIGNER').map((c) => c.id)).toEqual(['EN_ATTENTE_SIGNATURE'])
    expect(contratsOnglet(liste, 'EN_COURS').map((c) => c.id)).toEqual([
      'SIGNE',
      'ENGAGEMENT_VALIDE',
      'EN_LIVRAISON',
    ])
    expect(contratsOnglet(liste, 'LIVRES').map((c) => c.id)).toEqual(['LIVRE'])
    expect(contratsOnglet(liste, 'CLOTURES').map((c) => c.id)).toEqual(['CLOTURE'])
    expect(contratsOnglet(liste, null)).toHaveLength(liste.length)
  })
  it('contrats actifs : ni brouillon ni cloture', () => {
    expect(contratsActifs(liste)).toBe(5)
  })
})
