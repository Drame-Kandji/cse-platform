import { EyeOff, Inbox, Plus } from 'lucide-react'
import { useState } from 'react'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { formatDateTime, formatMoney, formatNumber, EMPTY_VALUE } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import type { AppelOffre, Offre } from '../api/types'
import { useOffres } from '../hooks/useAppelsOffres'
import { dateLimiteDepassee, nomOffre } from './offre-display'
import { useNow } from './decompte'
import { SaisirOffreDialog } from './SaisirOffreDialog'

function colonnes(ao: AppelOffre): ColumnDef<Offre>[] {
  const anonyme = ao.statut === 'PUBLIE'
  const cols: ColumnDef<Offre>[] = [
    {
      id: 'offre',
      header: anonyme ? 'Code anonyme' : 'Fournisseur',
      meta: { mobilePrimary: true, label: 'Offre' },
      cell: ({ row }) => (
        <span className="flex items-center gap-2 font-medium">
          {nomOffre(row.original, ao.statut)}
          {row.original.retenue && <Badge tone="success">Retenue</Badge>}
        </span>
      ),
    },
    {
      accessorKey: 'montantPropose',
      header: 'Montant proposé',
      meta: { align: 'right' },
      cell: ({ row }) => formatMoney(row.original.montantPropose),
    },
    {
      accessorKey: 'delaiLivraisonJours',
      header: 'Délai',
      meta: { align: 'right' },
      cell: ({ row }) => `${formatNumber(row.original.delaiLivraisonJours)} j`,
    },
    {
      accessorKey: 'garantieMois',
      header: 'Garantie',
      meta: { align: 'right', hideOnMobile: true },
      cell: ({ row }) =>
        row.original.garantieMois == null ? EMPTY_VALUE : `${row.original.garantieMois} mois`,
    },
    {
      accessorKey: 'dateDepot',
      header: 'Dépôt',
      meta: { hideOnMobile: true },
      cell: ({ row }) => formatDateTime(row.original.dateDepot),
    },
  ]
  if (!anonyme) {
    cols.push({
      accessorKey: 'scoreGlobal',
      header: 'Score',
      meta: { align: 'right' },
      cell: ({ row }) =>
        row.original.scoreGlobal == null
          ? EMPTY_VALUE
          : `${formatNumber(row.original.scoreGlobal, { decimals: 1 })}/100`,
    })
  }
  return cols
}

/** Onglet Offres : liste anonymisee tant que l'AO est PUBLIE (R10), saisie hors portail (RA). */
export function OffresTab({ ao }: { ao: AppelOffre }) {
  const query = useOffres(ao.id)
  const [saisie, setSaisie] = useState(false)
  const now = useNow()
  const depassee = dateLimiteDepassee(ao.dateLimite, now)
  return (
    <div className="grid gap-4">
      {ao.statut === 'PUBLIE' && (
        <div className="flex flex-col gap-3 rounded-xl border border-info/30 bg-info/10 px-4 py-3 sm:flex-row sm:items-center">
          <EyeOff className="size-5 shrink-0 text-info-emphasis" aria-hidden />
          <p className="flex-1 text-sm text-info-emphasis">
            Les offres sont anonymisées jusqu&apos;à la clôture (R10) : seuls les codes anonymes sont
            visibles.
          </p>
          <Can roles={access.achatsGestion}>
            <div className="grid gap-1 sm:justify-items-end">
              <Button
                size="sm"
                leftIcon={<Plus />}
                disabled={depassee}
                onClick={() => {
                  setSaisie(true)
                }}
              >
                Saisir une offre reçue
              </Button>
              {depassee && (
                <p className="text-xs text-muted-foreground">
                  Date limite dépassée : aucune offre acceptée (R02).
                </p>
              )}
            </div>
          </Can>
        </div>
      )}
      <DataTable
        label="Offres reçues"
        columns={colonnes(ao)}
        data={query.data}
        getRowId={(offre) => offre.id}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        enableColumnVisibility={false}
        emptyState={
          <EmptyState
            icon={<Inbox aria-hidden />}
            title="Aucune offre reçue"
            description={
              ao.statut === 'PUBLIE'
                ? 'Les offres déposées sur le portail ou saisies par le RA apparaîtront ici.'
                : "Aucune offre n'a été déposée avant la date limite."
            }
          />
        }
      />
      <SaisirOffreDialog ao={ao} open={saisie} onOpenChange={setSaisie} />
    </div>
  )
}
