import { AlertTriangle, Copy, RotateCw, WifiOff } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { isApiError } from '@/shared/api/errors'
import { cn } from '@/shared/lib/cn'
import { Button } from './button'

export interface ErrorStateProps {
  error: unknown
  /** Relance (ex. `refetch` de TanStack Query). */
  onRetry?: () => void
  title?: string
  className?: string
  size?: 'default' | 'compact'
}

/**
 * Affiche une erreur (ProblemDetail RFC 7807 ou autre) de maniere lisible,
 * avec code, reference de trace copiable, erreurs de champs et bouton Reessayer.
 */
export function ErrorState({ error, onRetry, title, className, size = 'default' }: ErrorStateProps) {
  const { t } = useTranslation()
  const [copied, setCopied] = useState(false)
  const api = isApiError(error) ? error : null
  const heading = title ?? api?.title ?? t('states.error')
  const message = api ? api.detail : error instanceof Error ? error.message : t('states.unknownError')
  const reference = api?.traceId ?? api?.correlationId
  const Icon = api?.isNetworkError ? WifiOff : AlertTriangle

  const copy = () => {
    if (!reference) return
    void navigator.clipboard.writeText(reference).then(() => {
      setCopied(true)
    })
  }

  return (
    <div
      role="alert"
      className={cn(
        'flex flex-col items-center text-center',
        size === 'default' ? 'gap-3 px-6 py-12' : 'gap-2 px-4 py-6',
        className,
      )}
    >
      <div className="grid size-11 place-items-center rounded-xl bg-danger/10 text-danger-emphasis">
        <Icon className="size-5" aria-hidden />
      </div>
      <div className="grid max-w-md gap-1">
        <p className="text-sm font-semibold">{heading}</p>
        {message && message !== heading && <p className="text-sm text-muted-foreground">{message}</p>}
      </div>
      {api && api.fieldErrors.length > 0 && (
        <ul className="w-full max-w-md rounded-lg border border-border bg-surface-sunken p-3 text-left text-sm">
          {api.fieldErrors.map((fieldError) => (
            <li key={`${fieldError.field}-${fieldError.message}`} className="flex gap-2 py-0.5">
              <span className="font-medium">{fieldError.field}</span>
              <span className="text-muted-foreground">{fieldError.message}</span>
            </li>
          ))}
        </ul>
      )}
      {(api?.code ?? reference) && (
        <div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
          {api?.code && (
            <span>
              {t('error.code')} <span className="font-mono text-foreground/80">{api.code}</span>
            </span>
          )}
          {reference && (
            <button
              type="button"
              onClick={copy}
              className="inline-flex items-center gap-1 rounded px-1 hover:text-foreground"
              title={t('actions.copy')}
            >
              {t('error.trace')} <span className="font-mono">{reference.slice(0, 18)}</span>
              <Copy className="size-3" aria-hidden />
              <span className="sr-only">{copied ? t('actions.copied') : t('actions.copy')}</span>
            </button>
          )}
        </div>
      )}
      {onRetry && (
        <Button variant="secondary" size="sm" leftIcon={<RotateCw />} onClick={onRetry} className="mt-1">
          {t('actions.retry')}
        </Button>
      )}
    </div>
  )
}
