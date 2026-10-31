import { CheckCircle2, Flag } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { describeStatus } from '@/shared/domain/labels'
import { formatDate, toIsoDate } from '@/shared/lib/format'
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  ConfirmDialog,
  EmptyState,
  StatusBadge,
  Timeline,
  type TimelineItem,
} from '@/shared/ui'
import type { Contrat, Jalon } from '../api/types'
import { useMarquerJalon } from '../hooks/useContratActions'

/** Jalons de livraison ; le RA peut marquer un jalon atteint pendant la livraison. */
export function JalonsCard({ contrat, canMark }: { contrat: Contrat; canMark: boolean }) {
  const { t } = useTranslation('contrats')
  const mutation = useMarquerJalon(contrat.id)
  const jalons = [...contrat.jalons].sort((a, b) => a.datePrevue.localeCompare(b.datePrevue))

  const description = (jalon: Jalon) => (
    <div className="flex flex-wrap items-center gap-2">
      <StatusBadge kind="statutJalon" value={jalon.statut} size="sm" />
      <span className="tabular-nums">
        {jalon.dateReelle
          ? t('jalons.atteint', { date: formatDate(jalon.dateReelle) })
          : t('jalons.prevu', { date: formatDate(jalon.datePrevue) })}
      </span>
      {canMark && jalon.statut !== 'ATTEINT' && (
        <ConfirmDialog
          title={t('jalons.marquerTitle', { libelle: jalon.libelle })}
          description={t('jalons.marquerDescription')}
          onConfirm={async () => {
            await mutation.mutateAsync({ jalonId: jalon.id, dateReelle: toIsoDate(new Date()) ?? '' })
          }}
          trigger={
            <Button variant="secondary" size="sm" leftIcon={<CheckCircle2 />}>
              {t('jalons.marquer')}
            </Button>
          }
        />
      )}
    </div>
  )

  const items: TimelineItem[] = jalons.map((jalon) => ({
    id: jalon.id,
    title: jalon.libelle,
    tone: describeStatus('statutJalon', jalon.statut).tone,
    description: description(jalon),
  }))

  return (
    <Card>
      <CardHeader icon={<Flag aria-hidden />} title={t('jalons.title')} />
      <CardContent>
        {items.length ? <Timeline items={items} /> : <EmptyState size="compact" title={t('jalons.empty')} />}
      </CardContent>
    </Card>
  )
}
