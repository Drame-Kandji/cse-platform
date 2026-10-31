import { formatMoney, formatNumber, formatPercent } from '@/shared/lib/format'

/** Formate un taux exprime en pourcentage 0..100 (convention ms-reporting) : 87.5 -> "87,5 %". */
export function formatTaux(value: number | null | undefined, decimals = 1): string {
  return formatPercent(typeof value === 'number' ? value / 100 : value, { decimals })
}

/** Montant compact pour les axes et KPI : "1,3 Md FCFA". */
export const compactMoney = (value: number) => formatMoney(value, { compact: true })

/** Nombre compact pour les axes. */
export const compactNumber = (value: number) => formatNumber(value, { compact: true })
