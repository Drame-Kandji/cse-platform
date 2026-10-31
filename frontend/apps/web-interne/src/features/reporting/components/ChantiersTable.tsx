import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { cn } from '@/shared/lib/cn'
import { formatMoney, formatNumber, formatPercent } from '@/shared/lib/format'
import { Card, CardHeader } from '@/shared/ui/card'
import { formatTaux } from '../lib/format'
import type { LigneChantier } from '../types'

function UtilisationBar({ value }: { value: number }) {
  return (
    <span className="flex items-center justify-end gap-2">
      <span className="hidden h-1.5 w-16 overflow-hidden rounded-full bg-muted sm:block" aria-hidden>
        <span
          className="block h-full rounded-full bg-chart-1"
          style={{ width: `${Math.max(0, Math.min(100, value))}%` }}
        />
      </span>
      <span className="w-12 text-right tabular-nums">{formatTaux(value, 0)}</span>
    </span>
  )
}

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
    id: 'couts',
    accessorFn: (row) => row.coutExploitation + row.coutMaintenance,
    header: 'Coûts (expl. + maint.)',
    enableSorting: true,
    meta: { align: 'right', hideOnMobile: true },
    cell: ({ row }) =>
      formatMoney(row.original.coutExploitation + row.original.coutMaintenance, { compact: true }),
  },
  {
    accessorKey: 'marge',
    header: 'Marge',
    enableSorting: true,
    meta: { align: 'right' },
    cell: ({ row }) => {
      const { marge, montantMarche } = row.original
      return (
        <span className="grid justify-items-end">
          <span
            className={cn(
              'font-medium tabular-nums',
              marge < 0 ? 'text-danger-emphasis' : 'text-success-emphasis',
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
    cell: ({ row }) => <UtilisationBar value={row.original.tauxUtilisation} />,
  },
  {
    accessorKey: 'materielsAffectes',
    header: 'Matériels',
    enableSorting: true,
    meta: { align: 'right', hideOnMobile: true },
    cell: ({ row }) => formatNumber(row.original.materielsAffectes),
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
        description="Marge = montant du marché − engagements − coûts d'exploitation et de maintenance"
        className="pb-4"
      />
      <DataTable
        label="Indicateurs par chantier"
        columns={columns}
        data={chantiers}
        getRowId={(row) => row.chantierId}
        className="rounded-none border-0 border-t shadow-none"
      />
    </Card>
  )
}
