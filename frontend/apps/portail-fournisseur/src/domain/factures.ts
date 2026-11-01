import type { Contrat, Facture } from './types'

/** Taux de TVA par defaut au Senegal (18 %). */
export const TAUX_TVA_DEFAUT = 18

/**
 * TVA et TTC a partir d'un montant HT et d'un taux en pourcentage (FCFA entiers, TVA arrondie).
 * Entrees invalides ou negatives : 0.
 * @example calculerTtc(1_000_000, 18) // { ht: 1000000, tva: 180000, ttc: 1180000 }
 */
export function calculerTtc(ht: number, tauxPct: number): { ht: number; tva: number; ttc: number } {
  const base = Number.isFinite(ht) && ht > 0 ? Math.round(ht) : 0
  const taux = Number.isFinite(tauxPct) && tauxPct > 0 ? tauxPct : 0
  const tva = Math.round((base * taux) / 100)
  return { ht: base, tva, ttc: base + tva }
}

/** Factures qui comptent (une facture rejetee doit etre remplacee). */
export function factureActive(facture: Pick<Facture, 'statut'>): boolean {
  return facture.statut !== 'REJETEE'
}

/** Vrai si le contrat a deja une facture non rejetee. */
export function contratFacture(contratId: string, factures: readonly Facture[]): boolean {
  return factures.some((f) => f.contratId === contratId && factureActive(f))
}

/** Contrats sur lesquels une facture peut etre deposee : LIVRE uniquement (CLOTURE est solde, §13). */
export function contratsFacturables(contrats: readonly Contrat[]): Contrat[] {
  return contrats.filter((c) => c.statut === 'LIVRE')
}

/** Totaux affiches sur l'accueil : facture (hors rejets) et paye, en TTC. */
export function totauxFactures(factures: readonly Facture[]): { facture: number; paye: number } {
  return factures.reduce(
    (acc, f) => ({
      facture: acc.facture + (factureActive(f) ? f.montantTtc : 0),
      paye: acc.paye + (f.statut === 'PAYEE' ? f.montantTtc : 0),
    }),
    { facture: 0, paye: 0 },
  )
}
