import { appelOffre, contrat, facture } from '@/test/fixtures'
import { deriverAFaire } from './a-faire'

describe('deriverAFaire', () => {
  it('liste vide sans donnees', () => {
    expect(deriverAFaire({ appelsOffres: [], contrats: [], factures: [] })).toEqual([])
  })

  it('invitations sans offre uniquement', () => {
    const items = deriverAFaire({
      appelsOffres: [
        appelOffre({ id: 'a', invite: true }),
        appelOffre({ id: 'b', invite: true, offreDeposee: true }),
        appelOffre({ id: 'c', invite: false }),
        appelOffre({ id: 'd', type: 'DEMANDE_PRIX' }),
      ],
      contrats: [],
      factures: [],
    })
    expect(items.map((i) => i.id)).toEqual(['ao-a', 'ao-d'].sort((x, y) => x.localeCompare(y)))
    expect(items[0]).toMatchObject({ type: 'OFFRE', lien: '/appels-offres/a' })
  })

  it('contrats a signer, a expedier, livres sans facture ; trie par priorite', () => {
    const items = deriverAFaire({
      appelsOffres: [appelOffre({ id: 'a', invite: true })],
      contrats: [
        contrat({ id: 'livre', statut: 'LIVRE' }),
        contrat({ id: 'exp', statut: 'ENGAGEMENT_VALIDE' }),
        contrat({ id: 'sig', statut: 'EN_ATTENTE_SIGNATURE' }),
        contrat({ id: 'cours', statut: 'EN_LIVRAISON' }),
        contrat({ id: 'facture', statut: 'LIVRE' }),
      ],
      factures: [facture({ contratId: 'facture' })],
    })
    expect(items.map((i) => i.type)).toEqual(['SIGNATURE', 'EXPEDITION', 'FACTURE', 'OFFRE'])
    expect(items[0]?.lien).toBe('/contrats/sig')
    expect(items[2]?.lien).toBe('/factures?nouvelle=livre')
  })

  it('facture rejetee non remplacee : une seule relance par contrat', () => {
    const items = deriverAFaire({
      appelsOffres: [],
      contrats: [contrat({ id: 'c1', statut: 'LIVRE' })],
      factures: [
        facture({ id: 'f1', contratId: 'c1', statut: 'REJETEE', motifRejet: 'Quantités non conformes' }),
        facture({ id: 'f2', contratId: 'c1', statut: 'REJETEE' }),
        facture({ id: 'f3', contratId: 'c2', statut: 'REJETEE' }),
        facture({ id: 'f4', contratId: 'c2', statut: 'RECUE' }),
      ],
    })
    expect(items).toEqual([
      expect.objectContaining({
        id: 'rej-f1',
        type: 'FACTURE_REJETEE',
        detail: 'FA-SE-2026-118 · Quantités non conformes',
        lien: '/factures?statut=REJETEE',
      }),
      // Contrat livre sans facture active : nouveau depot propose.
      expect.objectContaining({ id: 'fac-c1', type: 'FACTURE', lien: '/factures?nouvelle=c1' }),
    ])
  })
})
