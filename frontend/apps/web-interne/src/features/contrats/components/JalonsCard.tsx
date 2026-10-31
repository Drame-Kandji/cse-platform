import { useTranslation } from 'react-i18next'
import { formatDate, toIsoDate } from '@/shared/lib/format'
import { Button, Card, ConfirmDialog, EmptyState, StatusBadge } from '@/shared/ui'
import type { Contrat } from '../api/types'
import { useMarquerJalon } from '../hooks/useContratActions'

/** Jalons de livraison (liste compacte) ; le RA peut marquer un jalon atteint pendant la livraison. */
export function JalonsCard({ contrat, canMark }: { contrat: Contrat; canMark: boolean }) {
  const { t } = useTranslation('contrats')
  const mutation = useMarquerJalon(contrat.id)
  const jalons = [...contrat.jalons].sort((a, b) => a.datePrevue.localeCompare(b.datePrevue))

  if (jalons.length === 0) {
    return (
      <Card>
        <EmptyState size="compact" title={t('jalons.empty')} />
      </Card>
    )
  }
  return (
    <Card>
      <ul aria-label={t('jalons.title')} className="divide-y divide-border">
        {jalons.map((jalon) => (
          <li key={jalon.id} className="flex flex-wrap items-center gap-x-3 gap-y-2 px-4 py-3 sm:px-5">
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium">{jalon.libelle}</p>
              <p className="text-xs tabular-nums text-muted-foreground">
                {jalon.dateReelle
                  ? t('jalons.atteint', { date: formatDate(jalon.dateReelle) })
                  : t('jalons.prevu', { date: formatDate(jalon.datePrevue) })}
              </p>
            </div>
            <StatusBadge kind="statutJalon" value={jalon.statut} size="sm" />
            {canMark && jalon.statut !== 'ATTEINT' && (
              <ConfirmDialog
                title={t('jalons.marquerTitle', { libelle: jalon.libelle })}
                description={t('jalons.marquerDescription')}
                onConfirm={async () => {
                  await mutation.mutateAsync({ jalonId: jalon.id, dateReelle: toIsoDate(new Date()) ?? '' })
                }}
                trigger={
                  <Button variant="secondary" size="sm">
                    {t('jalons.marquer')}
                  </Button>
                }
              />
            )}
          </li>
        ))}
      </ul>
    </Card>
  )
}
