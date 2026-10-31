import type { ColumnDef } from '@tanstack/react-table'
import { ClipboardList, Download, PackageSearch, Truck, Wallet, Wrench } from 'lucide-react'
import { useMemo, useState } from 'react'
import { toast } from 'sonner'
import { ApiError } from '@/shared/api/errors'
import type { PageResponse } from '@/shared/api/types'
import { optionsOf, type EtatMateriel } from '@/shared/domain/labels'
import { formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DataTable, useTableState } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { Skeleton } from '@/shared/ui/skeleton'
import { Sparkline } from '@/shared/ui/sparkline'
import { StatCard } from '@/shared/ui/stat-card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { DEMO_MATERIELS, type DemoMateriel } from './demo-data'
import { Demo, Section } from './Section'

const columns: ColumnDef<DemoMateriel>[] = [
  {
    accessorKey: 'code',
    header: 'Code',
    enableSorting: true,
    enableHiding: false,
    cell: ({ row }) => <span className="font-mono text-xs font-medium">{row.original.code}</span>,
  },
  {
    accessorKey: 'designation',
    header: 'Désignation',
    enableSorting: true,
    meta: { mobilePrimary: true },
    cell: ({ row }) => <span className="font-medium">{row.original.designation}</span>,
  },
  {
    accessorKey: 'categorie',
    header: 'Catégorie',
    cell: ({ row }) => <StatusBadge kind="categorieMateriel" value={row.original.categorie} dot={false} />,
  },
  {
    accessorKey: 'etat',
    header: 'État',
    cell: ({ row }) => <StatusBadge kind="etatMateriel" value={row.original.etat} />,
  },
  {
    accessorKey: 'chantier',
    header: 'Chantier',
    meta: { hideOnMobile: true },
    cell: ({ row }) => row.original.chantier ?? <span className="text-muted-foreground">—</span>,
  },
  {
    accessorKey: 'miseEnService',
    header: 'Mise en service',
    enableSorting: true,
    cell: ({ row }) => formatDate(row.original.miseEnService),
  },
  {
    accessorKey: 'valeur',
    header: 'Valeur',
    enableSorting: true,
    meta: { align: 'right' },
    cell: ({ row }) => formatMoney(row.original.valeur),
  },
]

const ERROR_EXAMPLE = new ApiError(409, {
  title: 'Conflit de version',
  detail: 'Le matériel a été modifié par un autre utilisateur. Rechargez la fiche.',
  code: 'MATERIEL_VERSION_CONFLICT',
  traceId: '7f3c2a91e8b04d5f9a1c',
  errors: [{ field: 'numeroSerie', message: 'doit être unique' }],
})

function useDemoPage(etats: EtatMateriel[], search: string) {
  const state = useTableState({ prefix: 'demo_', defaultPageSize: 10 })
  const page = useMemo<PageResponse<DemoMateriel>>(() => {
    const term = search.toLowerCase()
    const filtered = DEMO_MATERIELS.filter(
      (m) =>
        (etats.length === 0 || etats.includes(m.etat)) &&
        (!term || `${m.code} ${m.designation}`.toLowerCase().includes(term)),
    )
    const sort = state.sorting[0]
    const sorted = sort
      ? [...filtered].sort(
          (a, b) =>
            String(a[sort.id as keyof DemoMateriel]).localeCompare(
              String(b[sort.id as keyof DemoMateriel]),
              'fr',
              { numeric: true },
            ) * (sort.desc ? -1 : 1),
        )
      : filtered
    const { page: index, size } = state.pageParams
    const totalPages = Math.ceil(sorted.length / size)
    return {
      content: sorted.slice(index * size, index * size + size),
      page: index,
      size,
      totalElements: sorted.length,
      totalPages,
      first: index === 0,
      last: index >= totalPages - 1,
    }
  }, [etats, search, state.sorting, state.pageParams])
  return { state, page }
}

/** Indicateurs, tableau de donnees, filtres et etats. */
export function DataSection() {
  const [etats, setEtats] = useState<EtatMateriel[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const { state, page } = useDemoPage(etats, search)
  return (
    <Section
      id="donnees"
      title="Données"
      description="KPI, DataTable (pagination serveur simulée, tri, colonnes), filtres et états."
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Disponibilité du parc"
          value="81,4 %"
          icon={<Truck />}
          delta={{ value: 0.034, label: 'vs mois dernier' }}
          sparkline={<Sparkline values={[71, 74, 73, 77, 76, 79, 81]} />}
        />
        <StatCard
          label="Besoins en attente"
          value={formatNumber(23)}
          icon={<ClipboardList />}
          iconTone="warning"
          delta={{ value: 0.12, label: 'vs mois dernier', positiveIsGood: false }}
          sparkline={<Sparkline className="text-chart-2" values={[12, 15, 14, 19, 18, 21, 23]} />}
        />
        <StatCard
          label="Coût maintenance (oct.)"
          value={formatMoney(5_380_000)}
          icon={<Wrench />}
          iconTone="info"
          delta={{ value: -0.081, label: 'vs sept.', positiveIsGood: false }}
          hint="3 ordres en cours"
        />
        <StatCard
          label="Budget engagé"
          value={formatMoney(1_840_000_000, { compact: true })}
          icon={<Wallet />}
          iconTone="success"
          loading={loading}
        />
      </div>
      <DataTable
        label="Parc matériel (démo)"
        columns={columns}
        data={loading ? undefined : page}
        isLoading={loading}
        getRowId={(row) => row.id}
        pagination={state.pagination}
        onPaginationChange={state.onPaginationChange}
        sorting={state.sorting}
        onSortingChange={state.onSortingChange}
        onRowClick={(row) => toast.info(`Ouvrir ${row.code}`)}
        toolbar={
          <FilterBar
            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Code ou désignation…"
            canReset={etats.length > 0 || search !== ''}
            onReset={() => {
              setEtats([])
              setSearch('')
            }}
          >
            <FilterChip label="État" options={optionsOf('etatMateriel')} value={etats} onChange={setEtats} />
          </FilterBar>
        }
      />
      <div className="flex flex-wrap gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => {
            setLoading((v) => !v)
          }}
        >
          {loading ? 'Arrêter le chargement' : 'Simuler le chargement'}
        </Button>
      </div>
      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-3">
        <Demo title="EmptyState" className="p-0">
          <EmptyState
            icon={<PackageSearch aria-hidden />}
            title="Aucun matériel trouvé"
            description="Aucun matériel ne correspond à ces filtres."
            action={
              <Button size="sm" variant="secondary">
                Effacer les filtres
              </Button>
            }
          />
        </Demo>
        <Demo title="ErrorState (ProblemDetail 409)" className="p-0">
          <ErrorState error={ERROR_EXAMPLE} onRetry={() => toast('Nouvelle tentative')} size="compact" />
        </Demo>
        <Demo title="Skeleton">
          <div className="grid gap-3">
            <Skeleton className="h-5 w-1/2" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-5/6" />
            <div className="flex gap-3 pt-2">
              <Skeleton className="size-10 rounded-full" />
              <div className="grid flex-1 gap-2">
                <Skeleton className="h-4" />
                <Skeleton className="h-4 w-2/3" />
              </div>
            </div>
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Download />}
              className="mt-2 justify-self-start"
              onClick={() => toast.error('Export indisponible hors connexion')}
            >
              Toast d'erreur
            </Button>
          </div>
        </Demo>
      </div>
    </Section>
  )
}
