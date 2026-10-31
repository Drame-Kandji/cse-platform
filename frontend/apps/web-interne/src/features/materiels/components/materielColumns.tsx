import type { Materiel } from '@/shared/domain/types'
import { EMPTY_VALUE, formatDate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import type { ColumnDef } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'

/**
 * Colonnes du parc (5) ; tri active uniquement sur les champs `TRIABLES` de MaterielController
 * (ms-suivi). Categorie, compteur d'heures et tarif sont consultables dans la fiche du materiel.
 */
export const materielColumns: ColumnDef<Materiel>[] = [
  {
    accessorKey: 'code',
    header: 'Code',
    enableSorting: true,
    enableHiding: false,
    meta: { nowrap: true },
    cell: ({ row }) => (
      <span className="font-mono text-xs tabular-nums text-muted-foreground">{row.original.code}</span>
    ),
  },
  {
    accessorKey: 'designation',
    header: 'Désignation',
    enableSorting: true,
    meta: { mobilePrimary: true, minWidth: 240 },
    cell: ({ row }) => <span className="line-clamp-1 font-medium">{row.original.designation}</span>,
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
          <Badge tone="danger" size="sm">
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
      <span className="line-clamp-1 text-muted-foreground">
        {row.original.chantierActuelNom ?? EMPTY_VALUE}
      </span>
    ),
  },
  {
    accessorKey: 'prochaineMaintenance',
    header: 'Prochaine maintenance',
    enableSorting: true,
    meta: { nowrap: true },
    // Rouge uniquement si la maintenance est due (le texte porte l'information, pas la couleur seule).
    cell: ({ row }) =>
      row.original.maintenanceDue ? (
        <span className="font-medium text-danger-emphasis">Maintenance due</span>
      ) : row.original.prochaineMaintenance ? (
        <span className="tabular-nums text-muted-foreground">
          {formatDate(row.original.prochaineMaintenance)}
        </span>
      ) : (
        EMPTY_VALUE
      ),
  },
]
