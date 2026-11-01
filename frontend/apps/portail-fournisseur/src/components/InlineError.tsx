import { AlertCircle } from 'lucide-react'
import { cn } from '@/shared/lib/cn'

/** Erreur de formulaire non rattachee a un champ (code metier, conflit). */
export function InlineError({ message, className }: { message: string | null; className?: string }) {
  if (!message) return null
  return (
    <p
      role="alert"
      className={cn(
        'flex items-start gap-2 rounded-lg border border-danger/30 bg-danger/[0.06] px-3 py-2.5 text-sm text-danger-emphasis',
        className,
      )}
    >
      <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden />
      {message}
    </p>
  )
}
