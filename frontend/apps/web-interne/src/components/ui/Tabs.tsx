import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

export type TabItem = {
  id: string
  label: string
  count?: number
  icon?: ReactNode
}

type Props = {
  items: TabItem[]
  value: string
  onChange: (value: string) => void
  className?: string
}

export function Tabs({ items, value, onChange, className }: Props) {
  return (
    <div className={cn('flex gap-1 overflow-x-auto rounded-xl border border-ink-200 bg-white p-1 shadow-sm', className)} role="tablist">
      {items.map((item) => {
        const active = item.id === value
        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(item.id)}
            className={cn(
              'inline-flex h-9 shrink-0 items-center gap-2 rounded-lg px-3 text-sm font-medium transition',
              active ? 'bg-brand-900 text-white shadow-sm' : 'text-ink-600 hover:bg-ink-100',
            )}
          >
            {item.icon}
            {item.label}
            {typeof item.count === 'number' ? (
              <span
                className={cn(
                  'rounded-full px-1.5 py-0.5 text-[10px] font-bold',
                  active ? 'bg-white/20 text-white' : 'bg-ink-100 text-ink-600',
                )}
              >
                {item.count}
              </span>
            ) : null}
          </button>
        )
      })}
    </div>
  )
}
