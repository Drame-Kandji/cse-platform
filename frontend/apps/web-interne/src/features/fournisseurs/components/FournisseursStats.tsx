import { AlertTriangle, BadgeCheck, Gauge, UserPlus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { formatNumber } from '@/shared/lib/format'
import { StatCard } from '@/shared/ui/stat-card'
import { useStatistiquesFournisseurs } from '../hooks/useFournisseursQueries'

/** Indicateurs cles des fournisseurs (masques silencieusement en cas d'erreur). */
export function FournisseursStats() {
  const { t } = useTranslation('fournisseurs')
  const query = useStatistiquesFournisseurs()
  if (query.isError) return null
  const stats = query.data
  return (
    <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        label={t('stats.actifs')}
        value={formatNumber(stats?.actifs)}
        hint={stats ? `sur ${formatNumber(stats.total)}` : undefined}
        icon={<BadgeCheck aria-hidden />}
        iconTone="success"
        loading={query.isPending}
      />
      <StatCard
        label={t('stats.enAttente')}
        value={formatNumber(stats?.enAttente)}
        icon={<UserPlus aria-hidden />}
        iconTone="warning"
        loading={query.isPending}
      />
      <StatCard
        label={t('stats.scoreMoyen')}
        value={`${formatNumber(stats?.scoreMoyen, { decimals: 1 })} / 100`}
        icon={<Gauge aria-hidden />}
        loading={query.isPending}
      />
      <StatCard
        label={t('stats.litiges')}
        value={formatNumber(stats?.litigesOuverts)}
        hint={stats ? t('stats.documentsExpires', { count: stats.documentsExpires }) : undefined}
        icon={<AlertTriangle aria-hidden />}
        iconTone="danger"
        loading={query.isPending}
      />
    </div>
  )
}
