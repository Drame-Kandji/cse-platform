import { labelOf } from '@/shared/domain/labels'
import type { Affectation, Maintenance, Panne } from '@/shared/domain/types'
import { EMPTY_VALUE, formatDate, formatDateTime, formatMoney } from '@/shared/lib/format'
import type { ColumnDef } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'

const periode = (debut: string, fin: string | null | undefined) =>
  `${formatDate(debut)} → ${fin ? formatDate(fin) : '…'}`

export const affectationColumns: ColumnDef<Affectation>[] = [
  {
    accessorKey: 'chantierNom',
    header: 'Chantier',
    meta: { mobilePrimary: true },
    cell: ({ row }) => <span className="font-medium">{row.original.chantierNom}</span>,
  },
  {
    id: 'periode',
    header: 'Période prévue',
    cell: ({ row }) => (
      <span className="tabular-nums">{periode(row.original.dateDebut, row.original.dateFinPrevue)}</span>
    ),
  },
  {
    accessorKey: 'dateFinReelle',
    header: 'Fin réelle',
    cell: ({ row }) => formatDate(row.original.dateFinReelle),
  },
  {
    accessorKey: 'statut',
    header: 'Statut',
    cell: ({ row }) => <StatusBadge kind="statutAffectation" value={row.original.statut} />,
  },
  {
    accessorKey: 'dateReception',
    header: 'Réception',
    meta: { hideOnMobile: true },
    cell: ({ row }) => (
      <div className="grid max-w-64">
        <span className="tabular-nums">{formatDateTime(row.original.dateReception)}</span>
        {row.original.observationsReception && (
          <span className="truncate text-xs text-muted-foreground" title={row.original.observationsReception}>
            {row.original.observationsReception}
          </span>
        )}
      </div>
    ),
  },
]

export const maintenanceColumns: ColumnDef<Maintenance>[] = [
  {
    accessorKey: 'numeroOt',
    header: 'OT',
    cell: ({ row }) => <span className="font-mono text-xs">{row.original.numeroOt}</span>,
  },
  {
    accessorKey: 'type',
    header: 'Type',
    cell: ({ row }) => <StatusBadge kind="typeMaintenance" value={row.original.type} dot={false} />,
  },
  {
    accessorKey: 'description',
    header: 'Description',
    meta: { mobilePrimary: true },
    cell: ({ row }) => (
      <span className="block max-w-80 truncate" title={row.original.description}>
        {row.original.description}
      </span>
    ),
  },
  {
    accessorKey: 'datePlanifiee',
    header: 'Planifiée le',
    cell: ({ row }) => formatDate(row.original.datePlanifiee),
  },
  {
    accessorKey: 'statut',
    header: 'Statut',
    cell: ({ row }) => <StatusBadge kind="statutMaintenance" value={row.original.statut} />,
  },
  {
    accessorKey: 'cout',
    header: 'Coût',
    meta: { align: 'right', hideOnMobile: true },
    cell: ({ row }) => formatMoney(row.original.cout),
  },
]

export const panneColumns: ColumnDef<Panne>[] = [
  {
    accessorKey: 'dateSignalement',
    header: 'Signalée le',
    cell: ({ row }) => <span className="tabular-nums">{formatDateTime(row.original.dateSignalement)}</span>,
  },
  {
    accessorKey: 'gravite',
    header: 'Gravité',
    cell: ({ row }) => <StatusBadge kind="gravitePanne" value={row.original.gravite} />,
  },
  {
    accessorKey: 'description',
    header: 'Description',
    meta: { mobilePrimary: true },
    cell: ({ row }) => (
      <span className="block max-w-80 truncate" title={row.original.description}>
        {row.original.description}
      </span>
    ),
  },
  {
    accessorKey: 'signaleParNom',
    header: 'Par',
    meta: { hideOnMobile: true },
    cell: ({ row }) => (
      <div className="grid">
        <span>{row.original.signaleParNom}</span>
        <span className="text-xs text-muted-foreground">{row.original.chantierNom ?? EMPTY_VALUE}</span>
      </div>
    ),
  },
  {
    accessorKey: 'statut',
    header: 'Statut',
    cell: ({ row }) => <StatusBadge kind="statutPanne" value={row.original.statut} />,
  },
]

/** Libelle d'un mouvement pour la timeline. */
export function mouvementTitle(type: string, origine?: string | null, destination?: string | null): string {
  const base = labelOf('typeMouvement', type)
  if (origine && destination) return `${base} : ${origine} → ${destination}`
  if (destination) return `${base} vers ${destination}`
  if (origine) return `${base} depuis ${origine}`
  return base
}
