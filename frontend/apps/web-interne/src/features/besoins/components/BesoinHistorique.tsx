import { useTranslation } from 'react-i18next'
import { describeStatus, labelOf } from '@/shared/domain/labels'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { Timeline, type TimelineItem } from '@/shared/ui/timeline'
import { useHistoriqueBesoin } from '../hooks/useBesoins'

/** Historique du besoin (GET /historique, chronologique) affiche du plus recent au plus ancien. */
export function BesoinHistorique({ besoinId }: { besoinId: string }) {
  const { t } = useTranslation('besoins')
  const query = useHistoriqueBesoin(besoinId)
  if (query.isPending) {
    return (
      <div className="grid gap-4" aria-busy>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    )
  }
  if (query.isError) {
    return <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
  }
  if (query.data.length === 0) return <EmptyState size="compact" title={t('detail.historiqueVide')} />
  const items: TimelineItem[] = [...query.data].reverse().map((event) => {
    const statut = describeStatus('statutBesoin', event.statut)
    // Ligne secondaire courte : l'auteur seulement (le titre porte deja l'action).
    return {
      id: event.id,
      title: labelOf('actionHistoriqueBesoin', event.action),
      description: event.commentaire ?? undefined,
      date: event.date,
      actor: event.auteurNom,
      tone: statut.tone,
    }
  })
  return <Timeline items={items} label={t('detail.historique')} />
}
