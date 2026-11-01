import { contrat, facture } from '@/test/fixtures'
import { calculerTtc, contratFacture, contratsFacturables, TAUX_TVA_DEFAUT, totauxFactures } from './factures'

describe('calculerTtc', () => {
  it('applique le taux de TVA par defaut (18 %)', () => {
    expect(calculerTtc(1_000_000, TAUX_TVA_DEFAUT)).toEqual({ ht: 1_000_000, tva: 180_000, ttc: 1_180_000 })
  })
  it('arrondit la TVA au franc', () => {
    expect(calculerTtc(333, 18)).toEqual({ ht: 333, tva: 60, ttc: 393 })
    expect(calculerTtc(1000, 18.5)).toEqual({ ht: 1000, tva: 185, ttc: 1185 })
  })
  it('taux nul ou entrees invalides', () => {
    expect(calculerTtc(5000, 0)).toEqual({ ht: 5000, tva: 0, ttc: 5000 })
    expect(calculerTtc(Number.NaN, 18)).toEqual({ ht: 0, tva: 0, ttc: 0 })
    expect(calculerTtc(-10, 18)).toEqual({ ht: 0, tva: 0, ttc: 0 })
    expect(calculerTtc(1000, Number.NaN)).toEqual({ ht: 1000, tva: 0, ttc: 1000 })
  })
})

describe('factures et contrats', () => {
  it('une facture rejetee ne compte pas comme facturation du contrat', () => {
    expect(contratFacture('ct-1', [facture({ statut: 'REJETEE' })])).toBe(false)
    expect(contratFacture('ct-1', [facture({ statut: 'MANDATEE' })])).toBe(true)
    expect(contratFacture('ct-2', [facture()])).toBe(false)
  })
  it('seuls les contrats livres sont facturables (cloture = solde)', () => {
    const liste = [
      contrat({ id: 'a', statut: 'LIVRE' }),
      contrat({ id: 'b', statut: 'CLOTURE' }),
      contrat({ id: 'c', statut: 'EN_LIVRAISON' }),
    ]
    expect(contratsFacturables(liste).map((c) => c.id)).toEqual(['a'])
  })
  it('totaux TTC : facture hors rejets, paye', () => {
    const totaux = totauxFactures([
      facture({ montantTtc: 100, statut: 'RECUE' }),
      facture({ montantTtc: 200, statut: 'PAYEE' }),
      facture({ montantTtc: 400, statut: 'REJETEE' }),
    ])
    expect(totaux).toEqual({ facture: 300, paye: 200 })
  })
})
