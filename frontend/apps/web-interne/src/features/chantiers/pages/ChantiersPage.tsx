import { Building2, LayoutGrid, Plus, Table2 } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { Can, useHasRole } from '@/shared/auth'
import { STATUT_CHANTIER, labelOf, type StatutChantier } from '@/shared/domain/labels'
import { Button, IconButton } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { DataTable, useTableState } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { FilterBar } from '@/shared/ui/filter-bar'
import { PageHeader } from '@/shared/ui/page-header'
import { Select } from '@/shared/ui/select'
import type { Chantier } from '../api/types'
import { chantierColumns } from '../components/chantier-columns'
import { ChantierDetailSheet } from '../components/ChantierDetailSheet'
import { ChantierFormDialog } from '../components/ChantierFormDialog'
import { ChantiersGrid } from '../components/ChantiersGrid'
import { useChantiers } from '../hooks/useChantiers'

const DEFAULT_SORT = [{ id: 'nom', desc: false }]
type Vue = 'cartes' | 'tableau'
const TOUS = 'TOUS'

/** Chantiers : tableau (par defaut) ou cartes, filtres, detail lateral (?chantier=id), creation / edition / cloture. */
export function Component() {
  const { t } = useTranslation('chantiers')
  const [params, setParams] = useSearchParams()
  const table = useTableState({ defaultSort: DEFAULT_SORT, defaultPageSize: 20 })
  const estChef = useHasRole(['CHEF_CHANTIER'])
  const [vue, setVue] = useState<Vue>('tableau')
  const [edition, setEdition] = useState<{ chantier: Chantier | null } | null>(null)

  const statutParam = params.get('statut')
  const statut = (STATUT_CHANTIER as readonly string[]).includes(statutParam ?? '')
    ? (statutParam as StatutChantier)
    : undefined
  const q = params.get('q') ?? undefined
  const selection = params.get('chantier')
  const query = useChantiers({ statut, q, ...table.pageParams })
  const columns = useMemo(() => chantierColumns(t), [t])

  const setParam = (key: string, value: string | undefined, resetPage = true) => {
    setParams(
      (current) => {
        const next = new URLSearchParams(current)
        if (value) next.set(key, value)
        else next.delete(key)
        if (resetPage) next.delete('page')
        return next
      },
      { replace: true },
    )
  }
  const ouvrir = (chantier: Chantier) => {
    setParam('chantier', chantier.id, false)
  }
  const filtresActifs = Boolean(statut ?? q)
  const reset = () => {
    setParams({}, { replace: true })
  }

  const nouveau = (
    <Can roles={access.chantierGestion}>
      <Button
        leftIcon={<Plus />}
        onClick={() => {
          setEdition({ chantier: null })
        }}
      >
        {t('page.nouveau')}
      </Button>
    </Can>
  )
  const empty = filtresActifs ? (
    <EmptyState
      title={t('page.emptyFilteredTitle')}
      description={t('page.emptyFilteredDescription')}
      action={
        <Button variant="secondary" onClick={reset}>
          {t('page.resetFilters')}
        </Button>
      }
    />
  ) : (
    <EmptyState
      icon={<Building2 aria-hidden />}
      title={t('page.emptyTitle')}
      description={estChef ? t('page.emptyChefDescription') : t('page.emptyDescription')}
      action={nouveau}
    />
  )

  const toolbar = (
    <FilterBar
      search={q ?? ''}
      onSearchChange={(value) => {
        setParam('q', value || undefined)
      }}
      searchPlaceholder={t('page.searchPlaceholder')}
      canReset={filtresActifs}
      onReset={reset}
      actions={
        <div className="flex items-center gap-1" role="group" aria-label={t('page.vue')}>
          <IconButton
            label={t('page.vueTableau')}
            icon={<Table2 />}
            variant={vue === 'tableau' ? 'subtle' : 'ghost'}
            aria-pressed={vue === 'tableau'}
            onClick={() => {
              setVue('tableau')
            }}
          />
          <IconButton
            label={t('page.vueCartes')}
            icon={<LayoutGrid />}
            variant={vue === 'cartes' ? 'subtle' : 'ghost'}
            aria-pressed={vue === 'cartes'}
            onClick={() => {
              setVue('cartes')
            }}
          />
        </div>
      }
    >
      <Select
        aria-label={t('page.statut')}
        className="sm:w-44"
        value={statut ?? TOUS}
        onValueChange={(value) => {
          setParam('statut', value === TOUS ? undefined : value)
        }}
        options={[
          { value: TOUS, label: t('page.statut') },
          ...STATUT_CHANTIER.map((s) => ({ value: s, label: labelOf('statutChantier', s) })),
        ]}
      />
    </FilterBar>
  )

  return (
    <>
      <PageHeader title={t('page.title')} actions={nouveau} />
      {vue === 'tableau' ? (
        <DataTable
          label={t('page.tableLabel')}
          columns={columns}
          data={query.data}
          getRowId={(c) => c.id}
          isLoading={query.isPending}
          isFetching={query.isFetching}
          error={query.error}
          onRetry={() => void query.refetch()}
          pagination={table.pagination}
          onPaginationChange={table.onPaginationChange}
          sorting={table.sorting}
          onSortingChange={table.onSortingChange}
          onRowClick={ouvrir}
          emptyState={empty}
          toolbar={toolbar}
        />
      ) : (
        <div className="grid gap-4">
          <Card className="px-4 py-3">{toolbar}</Card>
          {query.isError ? (
            <Card>
              <ErrorState error={query.error} onRetry={() => void query.refetch()} />
            </Card>
          ) : query.data?.content.length === 0 ? (
            <Card>{empty}</Card>
          ) : (
            <ChantiersGrid
              page={query.data}
              loading={query.isPending}
              pageIndex={table.pagination.pageIndex}
              pageSize={table.pagination.pageSize}
              onPageChange={(pageIndex) => {
                table.onPaginationChange((p) => ({ ...p, pageIndex }))
              }}
              onPageSizeChange={(pageSize) => {
                table.onPaginationChange({ pageIndex: 0, pageSize })
              }}
              onOpen={ouvrir}
            />
          )}
        </div>
      )}
      <ChantierDetailSheet
        chantierId={selection}
        onClose={() => {
          setParam('chantier', undefined, false)
        }}
        onEdit={(chantier) => {
          setEdition({ chantier })
        }}
      />
      <ChantierFormDialog
        open={edition !== null}
        chantier={edition?.chantier}
        onOpenChange={(open) => {
          if (!open) setEdition(null)
        }}
      />
    </>
  )
}
