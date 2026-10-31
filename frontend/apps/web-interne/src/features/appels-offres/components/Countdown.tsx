import { Clock } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { formatDateTime } from '@/shared/lib/format'
import { textToneClasses } from '@/shared/ui/tone'
import { decompte, useNow } from './decompte'

/** Decompte jusqu'a la date limite de depot des offres. */
export function Countdown({ dateLimite, className }: { dateLimite: string; className?: string }) {
  const now = useNow()
  const { label, tone } = decompte(dateLimite, now)
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 text-xs font-medium tabular-nums',
        textToneClasses[tone],
        className,
      )}
      title={`Date limite : ${formatDateTime(dateLimite)}`}
    >
      <Clock className="size-3.5 shrink-0" aria-hidden />
      {label}
    </span>
  )
}
