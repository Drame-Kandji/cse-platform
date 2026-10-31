import type { TFunction } from 'i18next'
import type { ColumnDef } from '@/shared/ui/data-table'
import { EMPTY_VALUE, formatNumber } from '@/shared/lib/format'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Chantier } from '../api/types'

/** Colonnes du tableau des chantiers (5 colonnes ; budget et periode dans le detail). */
export function chantierColumns(t: TFunction<'chantiers'>): ColumnDef<Chantier>[] {
  return [
    {
      accessorKey: 'nom',
      header: t('columns.nom'),
      enableSorting: true,
      enableHiding: false,
      meta: { mobilePrimary: true, minWidth: 220 },
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="line-clamp-1 font-medium">{row.original.nom}</p>
          <p className="line-clamp-1 text-xs text-muted-foreground">
            <span className="font-mono">{row.original.code}</span> · {row.original.localisation}
          </p>
        </div>
      ),
    },
    {
      accessorKey: 'region',
      header: t('columns.region'),
      meta: { nowrap: true, hideOnMobile: true },
      enableSorting: true,
    },
    {
      accessorKey: 'chefChantierNom',
      header: t('columns.chef'),
      meta: { hideOnMobile: true, nowrap: true },
      cell: ({ row }) => row.original.chefChantierNom ?? EMPTY_VALUE,
    },
    {
      accessorKey: 'nombreMateriels',
      header: t('columns.materiels'),
      meta: { align: 'right' },
      cell: ({ row }) => formatNumber(row.original.nombreMateriels),
    },
    {
      accessorKey: 'statut',
      header: t('columns.statut'),
      meta: { nowrap: true },
      cell: ({ row }) => <StatusBadge kind="statutChantier" value={row.original.statut} />,
    },
  ]
}
