import type { TFunction } from 'i18next'
import type { ColumnDef } from '@/shared/ui/data-table'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Besoin } from '../api/types'

/** Colonnes de la liste des besoins (tri sur les champs whitelistes cote API). */
export function besoinColumns(t: TFunction<'besoins'>): ColumnDef<Besoin>[] {
  return [
    {
      accessorKey: 'reference',
      header: t('columns.reference'),
      enableSorting: true,
      enableHiding: false,
      meta: { nowrap: true },
      cell: ({ row }) => (
        <span className="font-mono text-xs font-medium tabular-nums">{row.original.reference}</span>
      ),
    },
    {
      accessorKey: 'designation',
      header: t('columns.designation'),
      enableSorting: true,
      meta: { mobilePrimary: true, minWidth: 220 },
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="line-clamp-2 font-medium">{row.original.designation}</p>
          <p className="text-xs text-muted-foreground md:hidden">{row.original.chantierNom}</p>
        </div>
      ),
    },
    {
      accessorKey: 'chantierNom',
      header: t('columns.chantier'),
      meta: { hideOnMobile: true, minWidth: 160 },
      cell: ({ row }) => (
        <span className="line-clamp-2 text-muted-foreground">{row.original.chantierNom}</span>
      ),
    },
    {
      accessorKey: 'quantite',
      header: t('columns.quantite'),
      meta: { align: 'right' },
      cell: ({ row }) => formatNumber(row.original.quantite),
    },
    {
      accessorKey: 'priorite',
      header: t('columns.priorite'),
      enableSorting: true,
      meta: { nowrap: true },
      cell: ({ row }) => <StatusBadge kind="priorite" value={row.original.priorite} />,
    },
    {
      accessorKey: 'statut',
      header: t('columns.statut'),
      enableSorting: true,
      meta: { nowrap: true },
      cell: ({ row }) => <StatusBadge kind="statutBesoin" value={row.original.statut} />,
    },
    {
      accessorKey: 'dateBesoin',
      header: t('columns.dateBesoin'),
      enableSorting: true,
      meta: { hideOnMobile: true, nowrap: true },
      cell: ({ row }) => <span className="tabular-nums">{formatDate(row.original.dateBesoin)}</span>,
    },
    {
      accessorKey: 'createdAt',
      header: t('columns.createdAt'),
      enableSorting: true,
      meta: { hideOnMobile: true, nowrap: true, hideBelow: 'xl' },
      cell: ({ row }) => (
        <span className="tabular-nums text-muted-foreground">{formatDate(row.original.createdAt)}</span>
      ),
    },
  ]
}
