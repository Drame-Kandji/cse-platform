import { Pencil, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { labelOf, optionsOf, type CategorieMateriel } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Button, IconButton } from '@/shared/ui/button'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { PageHeader } from '@/shared/ui/page-header'
import { CountTabs } from '@/shared/ui/count-tabs'
import { TypeEnginDialog } from '../components/TypeEnginDialog'
import { useTypesEngins } from '../hooks/useTypesEngins'
import { normalizeText } from '../lib/text'
import type { TypeEngin } from '../types'

type Vue = 'ACTIFS' | 'SANS_SEUIL'

/** Type actif sans aucun seuil : aucune alerte preventive possible. */
function sansSeuilMaintenance(item: TypeEngin) {
  return item.actif && item.seuilMaintenanceHeures == null && item.seuilMaintenanceJours == null
}

function columns(onEdit: (typeEngin: TypeEngin) => void): ColumnDef<TypeEngin>[] {
  return [
    {
      accessorKey: 'libelle',
      header: 'Libellé',
      enableSorting: true,
      enableHiding: false,
      meta: { mobilePrimary: true },
      cell: ({ row }) => (
        <span className="grid">
          <span className="font-medium">{row.original.libelle}</span>
          <span className="font-mono text-xs text-muted-foreground">{row.original.code}</span>
        </span>
      ),
    },
    {
      accessorKey: 'categorie',
      header: 'Catégorie',
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => labelOf('categorieMateriel', row.original.categorie),
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
        <Badge size="sm" variant={row.original.actif ? 'soft' : 'outline'}>
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
  const [vue, setVue] = useState<Vue | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<TypeEngin | null>(null)

  const all = query.data
  const filtered = useMemo(() => {
    const needle = normalizeText(search)
    return (all ?? []).filter(
      (item) =>
        (categories.length === 0 || categories.includes(item.categorie)) &&
        (vue !== 'ACTIFS' || item.actif) &&
        (vue !== 'SANS_SEUIL' || sansSeuilMaintenance(item)) &&
        (!needle || normalizeText(`${item.code} ${item.libelle}`).includes(needle)),
    )
  }, [all, search, categories, vue])

  const openEditor = (typeEngin: TypeEngin | null) => {
    setEditing(typeEngin)
    setDialogOpen(true)
  }
  const compte = (predicate: (item: TypeEngin) => boolean) => (all ? all.filter(predicate).length : undefined)

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
      <div className="grid gap-4">
        <CountTabs
          label="Types d'engins"
          className="mb-0"
          loading={query.isPending}
          value={vue}
          onChange={setVue}
          items={[
            { value: null, label: 'Tous', count: all?.length },
            { value: 'ACTIFS', label: 'Actifs', count: compte((item) => item.actif) },
            { value: 'SANS_SEUIL', label: 'Sans seuil', count: compte(sansSeuilMaintenance) },
          ]}
        />
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
