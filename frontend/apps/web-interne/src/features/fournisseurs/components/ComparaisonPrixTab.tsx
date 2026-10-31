import { TrendingDown } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { optionsOf, labelOf, type CategorieMateriel } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatMoney } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { Skeleton } from '@/shared/ui/skeleton'
import type { ComparaisonPrix } from '../api/types'
import { useComparaisonPrix } from '../hooks/useFournisseursQueries'
import { ScoreBar } from './ScoreBar'

function ArticleCard({ article }: { article: ComparaisonPrix }) {
  const { t } = useTranslation('fournisseurs')
  const offres = [...article.offres].sort((a, b) => a.prixUnitaire - b.prixUnitaire)
  const stats = [
    { label: t('comparaison.min'), value: article.prixMin },
    { label: t('comparaison.moyen'), value: article.prixMoyen },
    { label: t('comparaison.max'), value: article.prixMax },
  ]
  return (
    <Card>
      <CardHeader
        title={article.designation}
        description={[article.reference, labelOf('categorieMateriel', article.categorie)]
          .filter(Boolean)
          .join(' · ')}
      />
      <CardContent className="grid gap-4">
        <dl className="grid grid-cols-3 gap-2 rounded-lg bg-surface-sunken p-3 text-center">
          {stats.map((stat) => (
            <div key={stat.label}>
              <dt className="text-xs text-muted-foreground">{stat.label}</dt>
              <dd className="text-sm font-semibold tabular-nums">{formatMoney(stat.value)}</dd>
            </div>
          ))}
        </dl>
        <ul className="grid gap-2">
          {offres.map((offre, index) => (
            <li
              key={offre.fournisseurId}
              className={cn(
                'grid gap-2 rounded-lg border p-3 sm:grid-cols-[1fr_auto_auto] sm:items-center sm:gap-4',
                index === 0 ? 'border-success/40 bg-success/5' : 'border-border',
              )}
            >
              <div className="flex min-w-0 flex-wrap items-center gap-2">
                <span className="truncate text-sm font-medium">{offre.raisonSociale}</span>
                {index === 0 && (
                  <Badge tone="success" size="sm">
                    <TrendingDown aria-hidden />
                    {t('comparaison.meilleurPrix')}
                  </Badge>
                )}
              </div>
              <ScoreBar score={offre.scorePerformance} className="sm:w-36" />
              <div className="text-sm sm:text-right">
                <p className="font-semibold tabular-nums">{formatMoney(offre.prixUnitaire)}</p>
                <p className="text-xs text-muted-foreground">
                  {t('comparaison.delai', { count: offre.delaiLivraisonJours })}
                </p>
              </div>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

/** Onglet "Comparaison de prix" : prix catalogue par article entre fournisseurs. */
export function ComparaisonPrixTab() {
  const { t } = useTranslation('fournisseurs')
  const [categorie, setCategorie] = useState<CategorieMateriel | undefined>()
  const [q, setQ] = useState('')
  const query = useComparaisonPrix({ categorie, q: q || undefined })

  return (
    <div className="grid gap-4">
      <FilterBar
        search={q}
        onSearchChange={setQ}
        searchPlaceholder={t('comparaison.search')}
        canReset={Boolean(categorie ?? q)}
        onReset={() => {
          setCategorie(undefined)
          setQ('')
        }}
      >
        <FilterChip
          label={t('filters.categorie')}
          options={optionsOf('categorieMateriel')}
          value={categorie ? [categorie] : []}
          onChange={(values) => {
            setCategorie(values.at(-1))
          }}
        />
      </FilterBar>
      {query.isPending ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {[0, 1].map((key) => (
            <Skeleton key={key} className="h-64 rounded-xl" />
          ))}
        </div>
      ) : query.isError ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : query.data.length === 0 ? (
        <EmptyState title={t('comparaison.empty')} description={t('comparaison.emptyDescription')} />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {query.data.map((article) => (
            <ArticleCard key={`${article.reference ?? ''}-${article.designation}`} article={article} />
          ))}
        </div>
      )}
    </div>
  )
}
