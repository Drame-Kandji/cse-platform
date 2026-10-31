import { ArrowRightLeft, CalendarRange } from 'lucide-react'
import { useState } from 'react'
import { useHasRole } from '@/shared/auth/context'
import { STATUT_AFFECTATION, labelOf } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { AffecterDialog, type MaterielAffectable } from '@/shared/suivi'
import { Button } from '@/shared/ui/button'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { ProgressBar, Skeleton } from '@/shared/ui/skeleton'
import { PlanningTimeline } from '../components/PlanningTimeline'
import { PlanningToolbar } from '../components/PlanningToolbar'
import { usePlanning, usePlanningState } from '../hooks/usePlanning'
import { BAR_CLASSES } from '../lib/styles'

interface AffectationDraft {
  readonly materiel?: MaterielAffectable
  readonly chantierId?: string
  readonly date?: Date
}

/** Planning des affectations : Gantt par chantier et materiel, zoom semaine / mois / trimestre. */
export function Component() {
  const planning = usePlanningState()
  const { state } = planning
  const query = usePlanning(state)
  const peutAffecter = useHasRole(['RESPONSABLE_MATERIEL'])
  const [draft, setDraft] = useState<AffectationDraft | null>(null)

  return (
    <>
      <PageHeader
        title="Planning des affectations"
        actions={
          peutAffecter && (
            <Button
              leftIcon={<ArrowRightLeft />}
              onClick={() => {
                setDraft({ chantierId: state.chantierId ?? undefined })
              }}
            >
              Affecter un matériel
            </Button>
          )
        }
      />
      <div className="overflow-hidden rounded-xl border border-border bg-surface shadow-card">
        <PlanningToolbar
          zoom={state.zoom}
          periode={state.periode}
          chantierId={state.chantierId}
          categorie={state.categorie}
          onZoomChange={planning.setZoom}
          onNavigate={planning.naviguer}
          onToday={planning.aujourdhui}
          onChantierChange={planning.setChantier}
          onCategorieChange={planning.setCategorie}
        />
        <ProgressBar active={query.isFetching && !query.isPending} />
        {query.isPending ? (
          <div className="grid gap-2 p-4" aria-busy>
            {Array.from({ length: 6 }, (_, index) => (
              <Skeleton key={index} className="h-10 w-full" />
            ))}
          </div>
        ) : query.isError ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        ) : query.groupes.length === 0 ? (
          <EmptyState
            icon={<CalendarRange aria-hidden />}
            title="Aucune affectation sur la période"
            description="Changez de période ou de filtres pour voir l'occupation du parc."
          />
        ) : (
          <PlanningTimeline
            groupes={query.groupes}
            zoom={state.zoom}
            periode={state.periode}
            onSlotClick={
              peutAffecter
                ? (ligne, chantierId, jour) => {
                    setDraft({
                      materiel: {
                        id: ligne.materielId,
                        code: ligne.materielCode,
                        designation: ligne.materielDesignation,
                      },
                      chantierId,
                      date: jour,
                    })
                  }
                : undefined
            }
          />
        )}
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-border px-4 py-2.5 text-xs text-muted-foreground">
          {STATUT_AFFECTATION.map((statut) => (
            <span key={statut} className="inline-flex items-center gap-1.5">
              <span className={cn('h-2.5 w-5 rounded-sm', BAR_CLASSES[statut])} aria-hidden />
              {labelOf('statutAffectation', statut)}
            </span>
          ))}
          <span className="inline-flex items-center gap-1.5">
            <span className="h-3 w-0.5 bg-danger" aria-hidden />
            Aujourd&apos;hui
          </span>
        </div>
      </div>
      <AffecterDialog
        open={draft !== null}
        onOpenChange={(open) => {
          if (!open) setDraft(null)
        }}
        materiel={draft?.materiel}
        defaultChantierId={draft?.chantierId}
        defaultDateDebut={draft?.date}
      />
    </>
  )
}
