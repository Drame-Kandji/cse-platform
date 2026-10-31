import { scoreSousSeuil } from '@/shared/domain/score'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'

/** Score de performance en chiffre simple (sur 100), rouge seulement sous le seuil d'alerte. */
export function ScoreBar({ score, className }: { score: number; className?: string }) {
  const alerte = scoreSousSeuil(score)
  return (
    <span
      className={cn('tabular-nums', alerte ? 'font-medium text-danger-emphasis' : 'text-foreground', className)}
      title={alerte ? 'Sous le seuil d’alerte' : undefined}
    >
      {formatNumber(score, { decimals: 1 })}
      {alerte && <span className="sr-only"> (sous le seuil d’alerte)</span>}
    </span>
  )
}
