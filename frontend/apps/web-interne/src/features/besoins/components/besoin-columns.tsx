import type { TFunction } from 'i18next'
import type { ColumnDef } from '@/shared/ui/data-table'
import { formatDate } from '@/shared/lib/format'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Besoin } from '../api/types'

/**
 * Colonnes de la liste des besoins (5 au plus, tri sur les champs whitelistes cote API).
 * Quantite, date souhaitee et demandeur restent consultables dans la fiche du besoin.
 */
export function besoinColumns(t: TFunction<'besoins'>): ColumnDef<Besoin>[] {
  return [
    {
      accessorKey: 'reference',
      header: t('columns.reference'),
      enableSorting: true,
      enableHiding: false,
      meta: { nowrap: true },
      cell: ({ row }) => (
        <span className="font-mono text-xs tabular-nums text-muted-foreground">{row.original.reference}</span>
      ),
    },
    {
      accessorKey: 'designation',
      header: t('columns.designation'),
      enableSorting: true,
      meta: { mobilePrimary: true, minWidth: 260 },
      cell: ({ row }) => (
        <div className="grid min-w-0">
          <span className="line-clamp-1 font-medium">{row.original.designation}</span>
          <span className="line-clamp-1 text-xs text-muted-foreground">{row.original.chantierNom}</span>
        </div>
      ),
    },
    {
      accessorKey: 'priorite',
      header: t('columns.priorite'),
      enableSorting: true,
      meta: { nowrap: true },
      // Seules les priorites qui demandent de l'attention sont signalees (Normale = rien).
      cell: ({ row }) =>
        row.original.priorite === 'NORMAL' ? null : (
          <StatusBadge kind="priorite" value={row.original.priorite} />
        ),
    },
    {
      accessorKey: 'statut',
      header: t('columns.statut'),
      enableSorting: true,
      meta: { nowrap: true },
      cell: ({ row }) => <StatusBadge kind="statutBesoin" value={row.original.statut} />,
    },
    {
      accessorKey: 'createdAt',
      header: t('columns.createdAt'),
      enableSorting: true,
      meta: { hideOnMobile: true, nowrap: true },
      cell: ({ row }) => (
        <span className="tabular-nums text-muted-foreground">{formatDate(row.original.createdAt)}</span>
      ),
    },
  ]
}
