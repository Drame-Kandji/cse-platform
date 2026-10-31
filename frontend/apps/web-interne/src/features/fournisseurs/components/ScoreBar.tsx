import type { Tone } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { dotToneClasses, textToneClasses } from '@/shared/ui/tone'

/** Ton associe a un score de performance (0..100). */
function scoreTone(score: number): Tone {
  if (score >= 75) return 'success'
  if (score >= 50) return 'warning'
  return 'danger'
}

/** Jauge horizontale du score de performance, valeur toujours affichee en texte. */
export function ScoreBar({ score, className }: { score: number; className?: string }) {
  const tone = scoreTone(score)
  const value = Math.max(0, Math.min(100, score))
  return (
    <div className={cn('flex min-w-28 items-center gap-2', className)}>
      <div
        role="meter"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(value)}
        aria-label="Score de performance"
        className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted"
      >
        <div className={cn('h-full rounded-full', dotToneClasses[tone])} style={{ width: `${value}%` }} />
      </div>
      <span className={cn('w-12 text-right text-sm font-medium tabular-nums', textToneClasses[tone])}>
        {formatNumber(score, { decimals: 1 })}
      </span>
    </div>
  )
}
