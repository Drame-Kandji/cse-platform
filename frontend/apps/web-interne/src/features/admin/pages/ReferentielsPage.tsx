import { Pencil, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { optionsOf, type CategorieMateriel } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Button, IconButton } from '@/shared/ui/button'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { PageHeader } from '@/shared/ui/page-header'
import { StatCard } from '@/shared/ui/stat-card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { TypeEnginDialog } from '../components/TypeEnginDialog'
import { useTypesEngins } from '../hooks/useTypesEngins'
import { normalizeText } from '../lib/text'
import type { TypeEngin } from '../types'

function columns(onEdit: (typeEngin: TypeEngin) => void): ColumnDef<TypeEngin>[] {
  return [
    {
      accessorKey: 'code',
      header: 'Code',
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => <span className="font-mono text-xs">{row.original.code}</span>,
    },
    {
      accessorKey: 'libelle',
      header: 'Libellé',
      enableSorting: true,
      enableHiding: false,
      meta: { mobilePrimary: true },
      cell: ({ row }) => <span className="font-medium">{row.original.libelle}</span>,
    },
    {
      accessorKey: 'categorie',
      header: 'Catégorie',
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => (
        <StatusBadge kind="categorieMateriel" value={row.original.categorie} dot={false} size="sm" />
      ),
    },
    {
      accessorKey: 'seuilMaintenanceHeures',
      header: 'Seuil heures',
      enableSorting: true,
      meta: { align: 'right' },
      cell: ({ row }) =>
        row.original.seuilMaintenanceHeures != null
          ? `${formatNumber(row.original.seuilMaintenanceHeures)} h`
          : EMPTY_VALUE,
    },
    {
      accessorKey: 'seuilMaintenanceJours',
      header: 'Seuil jours',
      enableSorting: true,
      meta: { align: 'right' },
      cell: ({ row }) =>
        row.original.seuilMaintenanceJours != null
          ? `${formatNumber(row.original.seuilMaintenanceJours)} j`
          : EMPTY_VALUE,
    },
    {
      accessorKey: 'actif',
      header: 'Statut',
      meta: { nowrap: true },
      cell: ({ row }) => (
        <Badge tone={row.original.actif ? 'success' : 'neutral'} dot size="sm">
          {row.original.actif ? 'Actif' : 'Inactif'}
        </Badge>
      ),
    },
    {
      id: 'actions',
      header: () => <span className="sr-only">Actions</span>,
      enableHiding: false,
      meta: { align: 'right' },
      cell: ({ row }) => (
        <IconButton
          label={`Modifier ${row.original.libelle}`}
          icon={<Pencil />}
          size="icon-sm"
          onClick={() => {
            onEdit(row.original)
          }}
        />
      ),
    },
  ]
}

/** Referentiels : types d'engins et seuils de maintenance preventive (ecriture ADMIN). */
export function Component() {
  const query = useTypesEngins()
  const [search, setSearch] = useState('')
  const [categories, setCategories] = useState<CategorieMateriel[]>([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<TypeEngin | null>(null)

  const all = query.data
  const filtered = useMemo(() => {
    const needle = normalizeText(search)
    return (all ?? []).filter(
      (item) =>
        (categories.length === 0 || categories.includes(item.categorie)) &&
        (!needle || normalizeText(`${item.code} ${item.libelle}`).includes(needle)),
    )
  }, [all, search, categories])

  const openEditor = (typeEngin: TypeEngin | null) => {
    setEditing(typeEngin)
    setDialogOpen(true)
  }
  const sansSeuil = (all ?? []).filter(
    (item) => item.actif && item.seuilMaintenanceHeures == null && item.seuilMaintenanceJours == null,
  ).length

  return (
    <>
      <PageHeader
        title="Référentiels"
        actions={
          <Button
            leftIcon={<Plus />}
            onClick={() => {
              openEditor(null)
            }}
          >
            Nouveau type d'engin
          </Button>
        }
      />
      <div className="grid gap-6">
        <div className="grid gap-4 sm:grid-cols-3">
          <StatCard label="Types d'engins" value={formatNumber(all?.length)} loading={query.isPending} />
          <StatCard
            label="Types actifs"
            value={formatNumber(all?.filter((item) => item.actif).length)}
            loading={query.isPending}
            iconTone="success"
          />
          <StatCard
            label="Actifs sans seuil de maintenance"
            value={formatNumber(sansSeuil)}
            hint={
              sansSeuil > 0 ? 'Aucune alerte préventive possible' : 'Tous les types actifs sont paramétrés'
            }
            loading={query.isPending}
          />
        </div>
        <DataTable
          label="Types d'engins"
          columns={columns(openEditor)}
          data={filtered}
          getRowId={(item) => item.id}
          isLoading={query.isPending}
          isFetching={query.isFetching}
          error={query.error}
          onRetry={() => void query.refetch()}
          toolbar={
            <FilterBar
              search={search}
              onSearchChange={setSearch}
              searchPlaceholder="Code ou libellé…"
              canReset={search !== '' || categories.length > 0}
              onReset={() => {
                setSearch('')
                setCategories([])
              }}
            >
              <FilterChip
                label="Catégorie"
                options={optionsOf('categorieMateriel')}
                value={categories}
                onChange={setCategories}
              />
            </FilterBar>
          }
        />
      </div>
      <TypeEnginDialog open={dialogOpen} onOpenChange={setDialogOpen} typeEngin={editing} />
    </>
  )
}
