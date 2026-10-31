import { ClipboardList, Lock, PenLine } from 'lucide-react'
import { useState } from 'react'
import { access } from '@/app/access'
import { useHasRole } from '@/shared/auth'
import { describeStatus } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import type { AppelOffre, Offre } from '../api/types'
import { useOffres } from '../hooks/useAppelsOffres'
import { EvaluerOffreDialog } from './EvaluerOffreDialog'
import { nomOffre } from './offre-display'

const CALCULES = new Set(['PRIX', 'DELAI'])

/** Onglet Evaluation (apres cloture) : notes par critere, PRIX/DELAI calcules. */
export function EvaluationTab({ ao }: { ao: AppelOffre }) {
  const peutNoter = useHasRole(access.offresEvaluation) && ao.statut === 'CLOTURE' && !ao.attribution
  const query = useOffres(ao.id, ao.statut !== 'PUBLIE')
  const [cible, setCible] = useState<Offre | null>(null)

  if (ao.statut === 'PUBLIE') {
    return (
      <EmptyState
        icon={<Lock aria-hidden />}
        title="Évaluation disponible après la clôture"
        description="Les offres ne peuvent être évaluées qu'une fois l'appel d'offres clôturé et l'anonymat levé."
      />
    )
  }

  const criteres = ao.criteres.filter((c) => c.poids > 0)
  const columns: ColumnDef<Offre>[] = [
    {
      id: 'offre',
      header: 'Offre',
      meta: { mobilePrimary: true },
      cell: ({ row }) => <span className="font-medium">{nomOffre(row.original, ao.statut)}</span>,
    },
    ...criteres.map<ColumnDef<Offre>>((c) => ({
      id: c.code,
      header: `${describeStatus('critereEvaluation', c.code).label} (${c.poids} %)`,
      meta: { align: 'right', label: describeStatus('critereEvaluation', c.code).label },
      cell: ({ row }) => {
        const note = row.original.notes[c.code]
        return (
          <span className={CALCULES.has(c.code) ? 'text-muted-foreground' : undefined}>
            {note == null ? EMPTY_VALUE : formatNumber(note, { decimals: 1 })}
            {CALCULES.has(c.code) && note != null && <span className="sr-only"> (calculé)</span>}
          </span>
        )
      },
    })),
    {
      accessorKey: 'scoreGlobal',
      header: 'Score pondéré',
      meta: { align: 'right' },
      cell: ({ row }) =>
        row.original.scoreGlobal == null ? (
          EMPTY_VALUE
        ) : (
          <strong className="tabular-nums">{formatNumber(row.original.scoreGlobal, { decimals: 1 })}</strong>
        ),
    },
  ]
  if (peutNoter) {
    columns.push({
      id: 'actions',
      header: '',
      meta: { align: 'right', label: 'Actions' },
      cell: ({ row }) => (
        <Button
          size="sm"
          variant="secondary"
          leftIcon={<PenLine />}
          onClick={() => {
            setCible(row.original)
          }}
          aria-label={`Noter ${nomOffre(row.original, ao.statut)}`}
        >
          Noter
        </Button>
      ),
    })
  }

  return (
    <div className="grid gap-3">
      <p className="text-sm text-muted-foreground">
        Notes sur 100. Le prix et le délai (en gris) sont calculés automatiquement à partir des offres ; la
        qualité technique, la garantie, le SAV et les références sont notés par la commission.
        {ao.statut === 'ATTRIBUE' && ' Appel d’offres attribué : évaluation figée.'}
      </p>
      <DataTable
        label="Évaluation des offres"
        columns={columns}
        data={query.data}
        getRowId={(offre) => offre.id}
        isLoading={query.isPending}
        error={query.error}
        onRetry={() => void query.refetch()}
        enableColumnVisibility={false}
        emptyState={<EmptyState icon={<ClipboardList aria-hidden />} title="Aucune offre à évaluer" />}
      />
      <EvaluerOffreDialog
        ao={ao}
        offre={cible}
        onClose={() => {
          setCible(null)
        }}
      />
    </div>
  )
}
