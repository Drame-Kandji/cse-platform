import { ClipboardList, Plus } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { access } from '@/app/access'
import { paths, to } from '@/app/routes'
import { Can } from '@/shared/auth'
import { optionsOf } from '@/shared/domain/labels'
import { toDate, toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Combobox } from '@/shared/ui/combobox'
import { DataTable, useTableState } from '@/shared/ui/data-table'
import { DateRangePicker } from '@/shared/ui/date-picker'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar } from '@/shared/ui/filter-bar'
import { PageHeader } from '@/shared/ui/page-header'
import { besoinColumns } from '../components/besoin-columns'
import { EnumFilter } from '../components/EnumFilter'
import { BesoinStats } from '../components/BesoinStats'
import { useBesoins, useChantiersSelectionnables } from '../hooks/useBesoins'
import { useBesoinFiltres } from '../lib/use-besoin-filtres'

const DEFAULT_SORT = [{ id: 'createdAt', desc: true }]

/** Liste paginee des besoins : onglets de statut, recherche + priorite, autres filtres dans "Filtres". */
export function Component() {
  const { t } = useTranslation('besoins')
  const navigate = useNavigate()
  const table = useTableState({ defaultSort: DEFAULT_SORT })
  const { filtres, update, reset, actifs } = useBesoinFiltres()
  const query = useBesoins({ ...filtres, ...table.pageParams })
  const chantiers = useChantiersSelectionnables()
  const columns = useMemo(() => besoinColumns(t), [t])
  // Le statut VALIDE n'a pas d'onglet : il n'est compte comme filtre secondaire que dans ce cas.
  const statutHorsOnglet = filtres.statut === 'VALIDE' ? filtres.statut : undefined
  const secondaires = [statutHorsOnglet, filtres.categorie, filtres.chantierId, filtres.du].filter(Boolean).length
  const chantierOptions = (chantiers.data?.content ?? []).map((c) => ({
    value: c.id,
    label: c.nom,
    description: c.code,
  }))

  const nouveau = (
    <Can roles={access.besoinNouveau}>
      <Button asChild>
        <Link to={paths.besoinNouveau}>
          <Plus aria-hidden />
          {t('list.nouveau')}
        </Link>
      </Button>
    </Can>
  )

  return (
    <>
      <PageHeader title={t('list.title')} actions={nouveau} />
      <BesoinStats
        actif={filtres.statut}
        onFilter={(statut) => {
          update({ statut })
        }}
      />
      <DataTable
        label={t('list.tableLabel')}
        columns={columns}
        data={query.data}
        getRowId={(b) => b.id}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        pagination={table.pagination}
        onPaginationChange={table.onPaginationChange}
        sorting={table.sorting}
        onSortingChange={table.onSortingChange}
        onRowClick={(b) => {
          navigate(to.besoinDetail(b.id))
        }}
        emptyState={
          actifs > 0 ? (
            <EmptyState
              title={t('list.emptyFilteredTitle')}
              description={t('list.emptyFilteredDescription')}
              action={
                <Button variant="secondary" onClick={reset}>
                  {t('list.resetFilters')}
                </Button>
              }
            />
          ) : (
            <EmptyState
              icon={<ClipboardList aria-hidden />}
              title={t('list.emptyTitle')}
              description={t('list.emptyDescription')}
              action={nouveau}
            />
          )
        }
        toolbar={
          <FilterBar
            search={filtres.q ?? ''}
            onSearchChange={(q) => {
              update({ q })
            }}
            searchPlaceholder={t('list.searchPlaceholder')}
            canReset={actifs > 0}
            onReset={reset}
            moreCount={secondaires}
            moreFilters={
              <>
                <EnumFilter
                  className="w-full"
                  label={t('filters.statut')}
                  options={optionsOf('statutBesoin')}
                  value={filtres.statut}
                  onChange={(statut) => {
                    update({ statut })
                  }}
                />
                <EnumFilter
                  className="w-full"
                  label={t('filters.categorie')}
                  options={optionsOf('categorieMateriel')}
                  value={filtres.categorie}
                  onChange={(categorie) => {
                    update({ categorie })
                  }}
                />
                {chantierOptions.length > 1 && (
                  <Combobox
                    placeholder={t('filters.tousChantiers')}
                    options={chantierOptions}
                    value={filtres.chantierId ?? null}
                    onValueChange={(chantierId) => {
                      update({ chantierId: chantierId ?? undefined })
                    }}
                  />
                )}
                <DateRangePicker
                  placeholder={t('filters.periode')}
                  value={{ from: toDate(filtres.du), to: toDate(filtres.au) }}
                  onChange={(range) => {
                    update({ du: toIsoDate(range.from), au: toIsoDate(range.to) })
                  }}
                />
              </>
            }
          >
            <EnumFilter
              label={t('filters.priorite')}
              options={optionsOf('priorite')}
              value={filtres.priorite}
              onChange={(priorite) => {
                update({ priorite })
              }}
            />
          </FilterBar>
        }
      />
    </>
  )
}
