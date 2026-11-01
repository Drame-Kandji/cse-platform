import { useTranslation } from 'react-i18next'
import { useCurrentUser } from '@/shared/auth/context'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { StatCard } from '@/shared/ui/stat-card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useProfil } from '@/api/queries'
import { AFaireList } from '../components/AFaireList'
import { useAccueil } from '../hooks/useAccueil'

/** Accueil : salutation, « À faire » et 4 indicateurs. */
export function Component() {
  const { t } = useTranslation('portail')
  const user = useCurrentUser()
  const profil = useProfil()
  const accueil = useAccueil()
  const nom = profil.data?.raisonSociale ?? user.name
  return (
    <>
      <PageHeader
        title={t('accueil.hello', { nom })}
        meta={profil.data && <StatusBadge kind="statutFournisseur" value={profil.data.statut} />}
      />
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <StatCard
          label={t('accueil.kpis.offres')}
          value={formatNumber(accueil.kpis.offresEnCours)}
          loading={accueil.isLoading}
        />
        <StatCard
          label={t('accueil.kpis.contrats')}
          value={formatNumber(accueil.kpis.contratsActifs)}
          loading={accueil.isLoading}
        />
        <StatCard
          label={t('accueil.kpis.facture')}
          value={formatMoney(accueil.kpis.montantFacture, { compact: true })}
          hint={t('accueil.kpis.ttc')}
          loading={accueil.isLoading}
        />
        <StatCard
          label={t('accueil.kpis.paye')}
          value={formatMoney(accueil.kpis.montantPaye, { compact: true })}
          hint={t('accueil.kpis.ttc')}
          loading={accueil.isLoading}
        />
      </div>
      <div className="mt-6">
        {accueil.error ? (
          <ErrorState error={accueil.error} onRetry={accueil.refetch} />
        ) : (
          <AFaireList items={accueil.aFaire} loading={accueil.isLoading} />
        )}
      </div>
    </>
  )
}
