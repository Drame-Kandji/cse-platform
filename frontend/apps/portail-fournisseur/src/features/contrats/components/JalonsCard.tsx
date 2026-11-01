import { useTranslation } from 'react-i18next'
import { formatDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Jalon } from '@/domain/types'

/** Jalons de livraison ; « Marquer atteint » seulement si le contrat est en livraison. */
export function JalonsCard({
  jalons,
  editable,
  onAtteint,
}: {
  jalons: readonly Jalon[]
  editable: boolean
  onAtteint: (jalon: Jalon) => void
}) {
  const { t } = useTranslation('portail')
  if (jalons.length === 0) return null
  return (
    <Card>
      <CardHeader title={t('contrats.jalons')} />
      <CardContent className="pt-2">
        <ul className="divide-y divide-border">
          {jalons.map((jalon) => (
            <li key={jalon.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3">
              <div className="grid min-w-0 flex-1 gap-0.5">
                <span className="text-sm font-medium">{jalon.libelle}</span>
                <span className="text-xs tabular-nums text-muted-foreground">
                  {jalon.dateReelle
                    ? t('contrats.jalonAtteintLe', { date: formatDate(jalon.dateReelle) })
                    : t('contrats.jalonPrevu', { date: formatDate(jalon.datePrevue) })}
                </span>
              </div>
              {editable && !jalon.dateReelle ? (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    onAtteint(jalon)
                  }}
                >
                  {t('contrats.marquerAtteint')}
                </Button>
              ) : (
                <StatusBadge kind="statutJalon" value={jalon.statut} />
              )}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
