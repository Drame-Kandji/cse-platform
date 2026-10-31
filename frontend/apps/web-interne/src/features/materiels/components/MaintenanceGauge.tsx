import type { Materiel } from '@/shared/domain/types'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { maintenanceRatio } from '../lib/compteurs'

function gaugeTone(ratio: number, due: boolean): string {
  if (due || ratio >= 1) return 'bg-danger'
  if (ratio >= 0.8) return 'bg-warning'
  return 'bg-success'
}

/**
 * Jauge "heures depuis la derniere maintenance" rapportee au seuil du type d'engin.
 * Le texte porte l'information (pas seulement la couleur).
 */
export function MaintenanceGauge({
  materiel,
  size = 'sm',
  className,
}: {
  materiel: Pick<Materiel, 'heuresDepuisMaintenance' | 'seuilMaintenanceHeures' | 'maintenanceDue'>
  size?: 'sm' | 'lg'
  className?: string
}) {
  const ratio = maintenanceRatio(materiel)
  const label =
    ratio === null
      ? `${formatNumber(materiel.heuresDepuisMaintenance)} h`
      : `${formatNumber(materiel.heuresDepuisMaintenance)} / ${formatNumber(materiel.seuilMaintenanceHeures)} h`
  return (
    <div className={cn('grid gap-1', size === 'sm' ? 'w-32' : 'w-full', className)}>
      <div
        className={cn(
          'flex items-baseline justify-between gap-2 tabular-nums',
          size === 'sm' ? 'text-xs' : 'text-sm',
        )}
      >
        <span className={cn(size === 'sm' ? 'text-muted-foreground' : 'font-medium text-foreground')}>
          {label}
        </span>
        {ratio !== null && (
          <span
            className={cn(
              'text-xs',
              ratio >= 1 ? 'font-medium text-danger-emphasis' : 'text-muted-foreground',
            )}
          >
            {Math.round(ratio * 100)} %
          </span>
        )}
      </div>
      {ratio !== null && (
        <div
          role="progressbar"
          aria-label="Heures depuis la dernière maintenance"
          aria-valuemin={0}
          aria-valuemax={materiel.seuilMaintenanceHeures ?? undefined}
          aria-valuenow={materiel.heuresDepuisMaintenance}
          className={cn('overflow-hidden rounded-full bg-muted', size === 'sm' ? 'h-1.5' : 'h-2.5')}
        >
          <div
            className={cn(
              'h-full rounded-full transition-[width]',
              gaugeTone(ratio, materiel.maintenanceDue),
            )}
            style={{ width: `${String(Math.min(100, Math.round(ratio * 100)))}%` }}
          />
        </div>
      )}
    </div>
  )
}
