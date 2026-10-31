import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { formatMoney, formatPercent } from '@/shared/lib/format'

/** Jauge budget / montant du marche (au-dela de 100 % : budget superieur au marche). */
export function BudgetBar({ budget, montantMarche }: { budget: number; montantMarche: number }) {
  const { t } = useTranslation('chantiers')
  const ratio = montantMarche > 0 ? budget / montantMarche : 0
  const depasse = ratio > 1
  return (
    <div className="grid gap-1.5">
      <div className="flex items-baseline justify-between gap-2 text-xs">
        <span className="text-muted-foreground">{t('card.budgetSurMarche')}</span>
        <span
          className={cn('font-medium tabular-nums', depasse ? 'text-danger-emphasis' : 'text-foreground')}
        >
          {t('card.ratio', { value: formatPercent(ratio, { decimals: 0 }) })}
        </span>
      </div>
      <div
        className="h-1.5 overflow-hidden rounded-full bg-muted"
        role="meter"
        aria-label={t('card.budgetSurMarche')}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(Math.min(ratio, 1) * 100)}
      >
        <div
          className={cn(
            'h-full rounded-full',
            depasse ? 'bg-danger' : ratio > 0.85 ? 'bg-warning' : 'bg-primary',
          )}
          style={{ width: `${Math.min(ratio, 1) * 100}%` }}
        />
      </div>
      <div className="flex justify-between gap-2 text-xs tabular-nums text-muted-foreground">
        <span>{formatMoney(budget, { compact: true })}</span>
        <span>{formatMoney(montantMarche, { compact: true })}</span>
      </div>
    </div>
  )
}
