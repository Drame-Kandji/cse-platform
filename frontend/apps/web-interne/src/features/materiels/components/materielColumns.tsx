import { AlertTriangle, Clock } from 'lucide-react'
import { labelOf } from '@/shared/domain/labels'
import type { Materiel } from '@/shared/domain/types'
import { EMPTY_VALUE, formatDate, formatMoney } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import type { ColumnDef } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { MaintenanceGauge } from './MaintenanceGauge'

/** Colonnes du parc ; tri active uniquement sur les champs `TRIABLES` de MaterielController (ms-suivi). */
export const materielColumns: ColumnDef<Materiel>[] = [
  {
    accessorKey: 'code',
    header: 'Code',
    enableSorting: true,
    enableHiding: false,
    meta: { nowrap: true },
    cell: ({ row }) => (
      <div className="grid">
        <span className="font-mono text-xs font-medium text-foreground">{row.original.code}</span>
        {row.original.immatriculation && (
          <span className="text-xs text-muted-foreground">{row.original.immatriculation}</span>
        )}
      </div>
    ),
  },
  {
    accessorKey: 'designation',
    header: 'Désignation',
    enableSorting: true,
    meta: { mobilePrimary: true, minWidth: 240 },
    cell: ({ row }) => {
      const m = row.original
      const sub = [m.marque, m.modele].filter(Boolean).join(' ') || m.typeEnginLibelle
      return (
        <div className="grid min-w-0">
          <span className="line-clamp-2 font-medium text-foreground">{m.designation}</span>
          <span className="line-clamp-1 text-xs text-muted-foreground">
            {sub ?? labelOf('categorieMateriel', m.categorie)}
          </span>
        </div>
      )
    },
  },
  {
    accessorKey: 'categorie',
    header: 'Catégorie',
    enableSorting: true,
    meta: { hideOnMobile: true, nowrap: true, hideBelow: '2xl' },
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {labelOf('categorieMateriel', row.original.categorie)}
      </span>
    ),
  },
  {
    accessorKey: 'etat',
    header: 'État',
    enableSorting: true,
    meta: { nowrap: true },
    cell: ({ row }) => (
      <div className="flex flex-wrap items-center gap-1.5">
        <StatusBadge kind="etatMateriel" value={row.original.etat} />
        {row.original.reformeDemandee && row.original.etat !== 'REFORME' && (
          <Badge tone="danger" variant="outline" size="sm">
            Réforme demandée
          </Badge>
        )}
      </div>
    ),
  },
  {
    accessorKey: 'chantierActuelNom',
    header: 'Chantier',
    meta: { label: 'Chantier actuel', minWidth: 160 },
    cell: ({ row }) => (
      <span className="line-clamp-2 text-sm">{row.original.chantierActuelNom ?? EMPTY_VALUE}</span>
    ),
  },
  {
    id: 'utilisation',
    header: 'Depuis maintenance',
    meta: { label: 'Heures depuis maintenance', hideOnMobile: true, nowrap: true, hideBelow: '2xl' },
    cell: ({ row }) => <MaintenanceGauge materiel={row.original} />,
  },
  {
    accessorKey: 'prochaineMaintenance',
    header: 'Prochaine maintenance',
    enableSorting: true,
    meta: { hideOnMobile: true, nowrap: true },
    cell: ({ row }) =>
      row.original.maintenanceDue ? (
        <Badge tone="danger" size="sm">
          <AlertTriangle aria-hidden />
          Maintenance due
        </Badge>
      ) : row.original.prochaineMaintenance ? (
        <span className="inline-flex items-center gap-1.5 text-sm tabular-nums text-muted-foreground">
          <Clock className="size-3.5" aria-hidden />
          {formatDate(row.original.prochaineMaintenance)}
        </span>
      ) : (
        EMPTY_VALUE
      ),
  },
  {
    accessorKey: 'tarifJournalier',
    header: 'Tarif / jour',
    enableSorting: true,
    meta: { align: 'right', hideOnMobile: true },
    cell: ({ row }) => formatMoney(row.original.tarifJournalier),
  },
]
