import { useTranslation } from 'react-i18next'
import type { StatutBesoin } from '@/shared/domain/labels'
import { CountTabs } from '@/shared/ui/count-tabs'
import { useStatistiquesBesoins } from '../hooks/useBesoins'

interface Props {
  /** Filtre rapide sur un statut ; undefined = tous. */
  onFilter: (statut: StatutBesoin | undefined) => void
  actif: StatutBesoin | undefined
}

/**
 * Onglets de statut avec compteurs (GET /api/besoins/statistiques) :
 * Tous / En attente / En achat / Affectés / Rejetés. VALIDE (transitoire, orientation en cours)
 * reste accessible via le filtre Statut du bouton "Filtres". Re-cliquer l'onglet actif revient a "Tous".
 */
export function BesoinStats({ onFilter, actif }: Props) {
  const { t } = useTranslation('besoins')
  const query = useStatistiquesBesoins()
  const stats = query.isError ? undefined : query.data
  const count = (statut: StatutBesoin) => (stats ? (stats.parStatut[statut] ?? 0) : undefined)
  return (
    <CountTabs<StatutBesoin>
      label={t('stats.parStatut')}
      loading={query.isPending}
      value={actif ?? null}
      onChange={(statut) => {
        onFilter(statut ?? undefined)
      }}
      items={[
        { value: null, label: t('stats.tous'), count: stats?.total },
        { value: 'EN_ATTENTE', label: t('stats.enAttente'), count: count('EN_ATTENTE') },
        { value: 'EN_ACHAT', label: t('stats.enAchat'), count: count('EN_ACHAT') },
        { value: 'AFFECTE_DIRECT', label: t('stats.affectes'), count: count('AFFECTE_DIRECT') },
        { value: 'REJETE', label: t('stats.rejetes'), count: count('REJETE') },
      ]}
    />
  )
}
