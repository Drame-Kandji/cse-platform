import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface InfoItem {
  readonly label: string
  readonly value: ReactNode
  /** Masque la ligne si la valeur est vide. */
  readonly hidden?: boolean
}

/** Liste libelle / valeur sobre (faits cles d'un detail). */
export function InfoList({ items, className }: { items: readonly InfoItem[]; className?: string }) {
  return (
    <dl className={cn('grid gap-3 text-sm', className)}>
      {items
        .filter((item) => !item.hidden)
        .map((item) => (
          <div key={item.label} className="flex items-baseline justify-between gap-4">
            <dt className="shrink-0 text-muted-foreground">{item.label}</dt>
            <dd className="min-w-0 text-right font-medium tabular-nums [overflow-wrap:anywhere]">
              {item.value}
            </dd>
          </div>
        ))}
    </dl>
  )
}
