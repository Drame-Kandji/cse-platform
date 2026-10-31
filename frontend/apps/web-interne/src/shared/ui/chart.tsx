import type { ReactElement, ReactNode } from 'react'
import { ResponsiveContainer, type TooltipProps } from 'recharts'
import type { NameType, ValueType } from 'recharts/types/component/DefaultTooltipContent'
import { cn } from '@/shared/lib/cn'

export interface ChartContainerProps {
  /** Description accessible du graphique (ce qu'il montre). */
  label: string
  height?: number
  className?: string
  children: ReactElement
}

/** Conteneur responsive d'un graphique Recharts, avec libelle accessible. */
export function ChartContainer({ label, height = 260, className, children }: ChartContainerProps) {
  return (
    <figure aria-label={label} className={cn('w-full text-xs', className)} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        {children}
      </ResponsiveContainer>
    </figure>
  )
}

export interface ChartTooltipContentProps extends TooltipProps<ValueType, NameType> {
  /** Formateur des valeurs (formatMoney, formatNumber...). */
  valueFormatter?: (value: number) => string
  labelFormatter?: (label: string) => ReactNode
}

/** Infobulle Recharts aux couleurs du design system : `<Tooltip content={<ChartTooltipContent />} />`. */
export function ChartTooltipContent({
  active,
  payload,
  label,
  valueFormatter,
  labelFormatter,
}: ChartTooltipContentProps) {
  if (!active || !payload?.length) return null
  return (
    <div className="min-w-40 rounded-lg border border-border bg-surface-raised px-3 py-2 text-xs shadow-overlay">
      {label !== undefined && (
        <p className="mb-1.5 font-medium text-foreground">
          {labelFormatter ? labelFormatter(String(label)) : String(label)}
        </p>
      )}
      <ul className="grid gap-1">
        {payload.map((item) => (
          <li key={String(item.dataKey ?? item.name)} className="flex items-center justify-between gap-4">
            <span className="flex items-center gap-1.5 text-muted-foreground">
              <span className="size-2 rounded-sm" style={{ backgroundColor: item.color }} aria-hidden />
              {item.name}
            </span>
            <span className="font-medium tabular-nums text-foreground">
              {typeof item.value === 'number' && valueFormatter
                ? valueFormatter(item.value)
                : String(item.value ?? '')}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Legende HTML (identite jamais portee par la seule couleur : libelles visibles). */
export function ChartLegend({
  items,
  className,
}: {
  items: readonly { label: string; color: string }[]
  className?: string
}) {
  return (
    <ul
      className={cn('flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground', className)}
    >
      {items.map((item) => (
        <li key={item.label} className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm" style={{ backgroundColor: item.color }} aria-hidden />
          {item.label}
        </li>
      ))}
    </ul>
  )
}
