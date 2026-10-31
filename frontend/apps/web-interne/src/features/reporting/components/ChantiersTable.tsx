import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { cn } from '@/shared/lib/cn'
import { formatMoney, formatNumber, formatPercent } from '@/shared/lib/format'
import { Card, CardHeader } from '@/shared/ui/card'
import { formatTaux } from '../lib/format'
import type { LigneChantier } from '../types'

const columns: ColumnDef<LigneChantier>[] = [
  {
    accessorKey: 'chantierNom',
    header: 'Chantier',
    enableSorting: true,
    enableHiding: false,
    meta: { mobilePrimary: true, minWidth: 200 },
    cell: ({ row }) => (
      <span className="grid">
        <span className="font-medium">{row.original.chantierNom}</span>
        <span className="text-xs text-muted-foreground">{row.original.region}</span>
      </span>
    ),
  },
  {
    accessorKey: 'budget',
    header: 'Budget',
    enableSorting: true,
    meta: { align: 'right', hideOnMobile: true },
    cell: ({ row }) => formatMoney(row.original.budget, { compact: true }),
  },
  {
    accessorKey: 'montantEngage',
    header: 'Engagé',
    enableSorting: true,
    meta: { align: 'right' },
    cell: ({ row }) => formatMoney(row.original.montantEngage, { compact: true }),
  },
  {
    accessorKey: 'marge',
    header: () => (
      <span title="Montant du marché − engagements − coûts d'exploitation et de maintenance">Marge</span>
    ),
    enableSorting: true,
    meta: { align: 'right', label: 'Marge' },
    cell: ({ row }) => {
      const { marge, montantMarche } = row.original
      return (
        <span className="grid justify-items-end">
          <span
            className={cn(
              'font-medium tabular-nums',
              marge < 0 && 'text-danger-emphasis',
            )}
          >
            {formatMoney(marge, { compact: true })}
          </span>
          {montantMarche > 0 && (
            <span className="text-2xs text-muted-foreground">
              {formatPercent(marge / montantMarche, { decimals: 0 })} du marché
            </span>
          )}
        </span>
      )
    },
  },
  {
    accessorKey: 'tauxUtilisation',
    header: 'Utilisation',
    enableSorting: true,
    meta: { align: 'right' },
    cell: ({ row }) => formatTaux(row.original.tauxUtilisation, 0),
  },
  {
    accessorKey: 'besoinsEnAttente',
    header: 'Besoins en attente',
    enableSorting: true,
    meta: { align: 'right', hideOnMobile: true },
    cell: ({ row }) => formatNumber(row.original.besoinsEnAttente),
  },
]

/** Tableau de rentabilite et d'utilisation par chantier (tri local). */
export function ChantiersTable({
  chantiers,
  title = 'Chantiers',
}: {
  chantiers: readonly LigneChantier[]
  title?: string
}) {
  return (
    <Card className="overflow-hidden">
      <CardHeader
        title={title}
        className="pb-4"
      />
      <DataTable
        label="Indicateurs par chantier"
        columns={columns}
        data={chantiers}
        getRowId={(row) => row.chantierId}
        enableColumnVisibility={false}
        className="rounded-none border-0 border-t shadow-none"
      />
    </Card>
  )
}
