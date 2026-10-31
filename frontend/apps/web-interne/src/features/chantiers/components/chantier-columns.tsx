import type { TFunction } from 'i18next'
import type { ColumnDef } from '@/shared/ui/data-table'
import { EMPTY_VALUE, formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Chantier } from '../api/types'

/** Colonnes du tableau des chantiers. */
export function chantierColumns(t: TFunction<'chantiers'>): ColumnDef<Chantier>[] {
  return [
    {
      accessorKey: 'code',
      header: t('columns.code'),
      meta: { nowrap: true },
      enableSorting: true,
      enableHiding: false,
      cell: ({ row }) => <span className="font-mono text-xs font-medium">{row.original.code}</span>,
    },
    {
      accessorKey: 'nom',
      header: t('columns.nom'),
      enableSorting: true,
      meta: { mobilePrimary: true, minWidth: 220 },
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="line-clamp-2 font-medium">{row.original.nom}</p>
          <p className="line-clamp-1 text-xs text-muted-foreground">{row.original.localisation}</p>
        </div>
      ),
    },
    {
      accessorKey: 'region',
      header: t('columns.region'),
      meta: { nowrap: true },
      enableSorting: true,
    },
    {
      accessorKey: 'statut',
      header: t('columns.statut'),
      meta: { nowrap: true },
      cell: ({ row }) => <StatusBadge kind="statutChantier" value={row.original.statut} />,
    },
    {
      accessorKey: 'chefChantierNom',
      header: t('columns.chef'),
      meta: { hideOnMobile: true, nowrap: true, hideBelow: '2xl' },
      cell: ({ row }) => row.original.chefChantierNom ?? EMPTY_VALUE,
    },
    {
      accessorKey: 'budget',
      header: t('columns.budget'),
      enableSorting: true,
      meta: { align: 'right' },
      cell: ({ row }) => formatMoney(row.original.budget),
    },
    {
      accessorKey: 'montantMarche',
      header: t('columns.montantMarche'),
      meta: { align: 'right', hideOnMobile: true, hideBelow: '2xl' },
      cell: ({ row }) => formatMoney(row.original.montantMarche),
    },
    {
      accessorKey: 'nombreMateriels',
      header: t('columns.materiels'),
      meta: { align: 'right' },
      cell: ({ row }) => formatNumber(row.original.nombreMateriels),
    },
    {
      accessorKey: 'dateDebut',
      header: t('columns.dateDebut'),
      enableSorting: true,
      meta: { hideOnMobile: true, nowrap: true },
      cell: ({ row }) => formatDate(row.original.dateDebut),
    },
  ]
}
