import { Clock } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { formatDateTime } from '@/shared/lib/format'
import { decompte } from '@/domain/offres'
import { useNow } from './use-now'

/** Temps restant avant la date limite (rouge sous 24 h). */
export function Countdown({ dateLimite, className }: { dateLimite: string; className?: string }) {
  const { label, urgent } = decompte(dateLimite, useNow())
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 text-xs tabular-nums',
        urgent ? 'font-medium text-danger-emphasis' : 'text-muted-foreground',
        className,
      )}
      title={formatDateTime(dateLimite)}
    >
      <Clock className="size-3.5 shrink-0" aria-hidden />
      {label}
    </span>
  )
}
