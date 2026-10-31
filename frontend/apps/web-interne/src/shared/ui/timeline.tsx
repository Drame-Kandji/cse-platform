import type { ReactNode } from 'react'
import type { Tone } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatDateTime, formatRelative } from '@/shared/lib/format'
import { dotToneClasses } from './tone'

export interface TimelineItem {
  readonly id: string
  readonly title: ReactNode
  readonly description?: ReactNode
  /** Date ISO de l'evenement. */
  readonly date?: string | Date
  readonly actor?: string
  readonly tone?: Tone
  readonly icon?: ReactNode
}

/**
 * Historique chronologique (changements de statut, commentaires).
 * Les elements sont affiches dans l'ordre fourni (le plus recent en premier conseille).
 */
export function Timeline({
  items,
  className,
  label,
}: {
  items: readonly TimelineItem[]
  className?: string
  /** Nom accessible de la liste (ex. « Historique »). */
  label?: string
}) {
  return (
    <ol aria-label={label} className={cn('relative grid gap-0', className)}>
      {items.map((item, index) => (
        <li key={item.id} className="relative flex gap-3 pb-5 last:pb-0">
          {index < items.length - 1 && (
            <span className="absolute left-[11px] top-7 h-[calc(100%-1.5rem)] w-px bg-border" aria-hidden />
          )}
          <span
            className={cn(
              'relative z-10 mt-0.5 grid size-6 shrink-0 place-items-center rounded-full border border-border bg-surface [&_svg]:size-3.5',
              item.icon ? 'text-muted-foreground' : '',
            )}
            aria-hidden
          >
            {item.icon ?? (
              <span className={cn('size-2 rounded-full', dotToneClasses[item.tone ?? 'neutral'])} />
            )}
          </span>
          <div className="min-w-0 flex-1 pt-0.5">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
              <p className="text-sm font-medium text-foreground">{item.title}</p>
              {item.date && (
                <time
                  className="text-xs tabular-nums text-muted-foreground"
                  dateTime={new Date(item.date).toISOString()}
                  title={formatDateTime(item.date)}
                >
                  {formatRelative(item.date)}
                </time>
              )}
            </div>
            {item.actor && <p className="text-xs text-muted-foreground">{item.actor}</p>}
            {item.description && (
              <div className="mt-1.5 text-sm text-muted-foreground">{item.description}</div>
            )}
          </div>
        </li>
      ))}
    </ol>
  )
}
