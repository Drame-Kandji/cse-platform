import { describe, expect, it } from 'vitest'
import { formatMoney } from '@/shared/lib/format'
import { yAxisWidth, yAxisWidthFor } from './chart-theme'

describe('largeur des axes Y', () => {
  it('laisse la place a "100 %" (graduation auparavant tronquee en "00 %")', () => {
    // 5 caracteres a ~7 px + marge de graduation : bien au-dela des 44 px d'origine.
    expect(yAxisWidth(['100 %'])).toBeGreaterThanOrEqual(48)
    expect(yAxisWidth(['100 %'])).toBeGreaterThan(yAxisWidth(['50 %']))
  })

  it('dimensionne selon la borne arrondie des valeurs formatees', () => {
    const compact = (v: number) => formatMoney(v, { compact: true })
    const width = yAxisWidthFor([12_000_000, 87_000_000], compact)
    expect(width).toBeGreaterThanOrEqual(yAxisWidth([compact(100_000_000)]))
    expect(yAxisWidthFor([], String)).toBe(yAxisWidth(['0']))
  })
})
