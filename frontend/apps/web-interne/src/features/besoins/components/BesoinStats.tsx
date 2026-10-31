import { CheckCircle2, ClipboardList, Hourglass, XCircle } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatutBesoin } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { StatCard } from '@/shared/ui/stat-card'
import { useStatistiquesBesoins } from '../hooks/useBesoins'

const NBSP = '\u00a0'

/** Formate un delai en heures : "18 h" ou "2,5 j". */
function formatDelai(heures: number): string {
  const valeur = heures < 48 ? formatNumber(heures) : formatNumber(heures / 24, { decimals: 1 })
  return `${valeur}${NBSP}${heures < 48 ? 'h' : 'j'}`
}

interface Props {
  /** Filtre rapide sur un statut (clic sur une carte) ; undefined = tous. */
  onFilter: (statut: StatutBesoin | undefined) => void
  actif: StatutBesoin | undefined
}

/** Bandeau de statistiques des besoins (GET /api/besoins/statistiques), cartes cliquables. */
export function BesoinStats({ onFilter, actif }: Props) {
  const { t } = useTranslation('besoins')
  const query = useStatistiquesBesoins()
  if (query.isError) return null
  const stats = query.data
  const count = (...statuts: StatutBesoin[]) =>
    statuts.reduce((sum, statut) => sum + (stats?.parStatut[statut] ?? 0), 0)
  const delai = stats?.delaiMoyenValidationHeures
  const critiques = stats?.parPriorite.CRITIQUE ?? 0

  /** `filtre` : statut applique au clic ; null = reinitialise ; absent = carte informative. */
  const cards: { key: string; filtre?: StatutBesoin | null; node: ReactNode }[] = [
    {
      key: 'total',
      filtre: null,
      node: (
        <StatCard
          label={t('stats.total')}
          value={formatNumber(stats?.total)}
          icon={<ClipboardList />}
          hint={critiques > 0 ? t('stats.critiques', { count: critiques }) : undefined}
          loading={query.isPending}
        />
      ),
    },
    {
      key: 'attente',
      filtre: 'EN_ATTENTE',
      node: (
        <StatCard
          label={t('stats.enAttente')}
          value={formatNumber(count('EN_ATTENTE'))}
          icon={<Hourglass />}
          iconTone="warning"
          hint={
            delai !== undefined && delai !== null
              ? t('stats.delai', { value: formatDelai(delai) })
              : t('stats.delaiInconnu')
          }
          loading={query.isPending}
        />
      ),
    },
    {
      key: 'satisfaits',
      node: (
        <StatCard
          label={t('stats.satisfaits')}
          value={formatNumber(count('VALIDE', 'AFFECTE_DIRECT', 'EN_ACHAT'))}
          icon={<CheckCircle2 />}
          iconTone="success"
          loading={query.isPending}
        />
      ),
    },
    {
      key: 'rejetes',
      filtre: 'REJETE',
      node: (
        <StatCard
          label={t('stats.rejetes')}
          value={formatNumber(count('REJETE'))}
          icon={<XCircle />}
          iconTone="danger"
          loading={query.isPending}
        />
      ),
    },
  ]

  return (
    <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-4">
      {cards.map((card) => {
        if (card.filtre === undefined) return <div key={card.key}>{card.node}</div>
        const filtre = card.filtre ?? undefined
        const active = filtre === actif
        return (
          <button
            key={card.key}
            type="button"
            title={t('stats.parStatut')}
            aria-pressed={active}
            onClick={() => {
              onFilter(active ? undefined : filtre)
            }}
            className={cn(
              'rounded-xl text-left transition-shadow focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              active && 'ring-2 ring-primary/40',
            )}
          >
            {card.node}
          </button>
        )
      })}
    </div>
  )
}
