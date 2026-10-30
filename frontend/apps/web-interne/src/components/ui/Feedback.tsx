import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Icon, type IconName } from './Icon'
import { Button } from './Button'

export function LoadingBlock({ label = 'Chargement des données CSE…', compact }: { label?: string; compact?: boolean }) {
  return (
    <div className={cn('flex items-center justify-center gap-3 text-sm text-ink-500', compact ? 'py-6' : 'py-16')}>
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-brand-200 border-t-brand-700" aria-hidden />
      {label}
    </div>
  )
}

export function ErrorBlock({
  message,
  onRetry,
  title = 'Impossible de charger les données',
}: {
  message: string
  onRetry?: () => void
  title?: string
}) {
  return (
    <div className="rounded-panel border border-danger-200 bg-danger-50 p-5">
      <div className="flex items-start gap-3">
        <Icon name="alert" className="mt-0.5 shrink-0 text-danger-600" />
        <div className="flex-1">
          <p className="text-sm font-semibold text-danger-700">{title}</p>
          <p className="mt-1 text-xs leading-relaxed text-danger-700/90">{message}</p>
          {onRetry ? (
            <Button variant="outline" size="sm" icon="refresh" className="mt-3" onClick={onRetry}>
              Réessayer
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  )
}

export function EmptyBlock({
  title = 'Aucun élément',
  message,
  icon = 'info',
  action,
}: {
  title?: string
  message?: string
  icon?: IconName
  action?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center">
      <span className="grid h-12 w-12 place-items-center rounded-full bg-ink-100 text-ink-400">
        <Icon name={icon} size={22} />
      </span>
      <p className="mt-3 text-sm font-semibold text-ink-700">{title}</p>
      {message ? <p className="mt-1 max-w-md text-xs leading-relaxed text-ink-500">{message}</p> : null}
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  )
}

export function InlineNotice({
  tone = 'info',
  title,
  children,
}: {
  tone?: 'info' | 'warning' | 'danger' | 'success'
  title?: string
  children: ReactNode
}) {
  const tones = {
    info: 'border-brand-200 bg-brand-50 text-brand-700',
    warning: 'border-accent-200 bg-accent-50 text-accent-700',
    danger: 'border-danger-200 bg-danger-50 text-danger-700',
    success: 'border-success-200 bg-success-50 text-success-700',
  }
  return (
    <div className={cn('rounded-lg border px-3 py-2.5 text-xs leading-relaxed', tones[tone])}>
      {title ? <p className="mb-0.5 font-semibold">{title}</p> : null}
      {children}
    </div>
  )
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-ink-100 px-4 py-3">
      <p className="text-xs text-ink-500">
        {totalElements} élément{totalElements > 1 ? 's' : ''} · page {page + 1} sur {Math.max(1, totalPages)}
      </p>
      <div className="flex items-center gap-1">
        <Button variant="outline" size="sm" icon="chevronLeft" disabled={page <= 0} onClick={() => onChange(page - 1)}>
          Précédent
        </Button>
        <Button
          variant="outline"
          size="sm"
          iconRight="chevronRight"
          disabled={page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        >
          Suivant
        </Button>
      </div>
    </div>
  )
}
