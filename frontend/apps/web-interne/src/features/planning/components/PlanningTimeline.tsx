import { Building2 } from 'lucide-react'
import type { MouseEvent } from 'react'
import { cn } from '@/shared/lib/cn'
import {
  graduations,
  jourAuPoint,
  nombreDeJours,
  positionBarre,
  positionJour,
  PX_PAR_JOUR,
  type Graduation,
  type Periode,
  type Zoom,
} from '../lib/layout'
import { finEffective, type GroupePlanning, type LignePlanning } from '../lib/rows'
import { AffectationBar } from './AffectationBar'

export interface PlanningTimelineProps {
  groupes: readonly GroupePlanning[]
  zoom: Zoom
  periode: Periode
  /** Clic sur un creneau vide d'une ligne (RM) ; absent = lecture seule. */
  onSlotClick?: (ligne: LignePlanning, chantierId: string, jour: Date) => void
  today?: Date
}

const LABEL_COL = 'w-44 shrink-0 sm:w-60'

/** Lignes verticales (jours/semaines), week-ends et repere du jour. */
function GridLines({ ticks, todayLeft }: { ticks: readonly Graduation[]; todayLeft: number | null }) {
  return (
    <div className="pointer-events-none absolute inset-0" aria-hidden>
      {ticks.map((tick) => (
        <span
          key={tick.key}
          className={cn('absolute inset-y-0 border-l border-border/60', tick.weekend && 'bg-muted/50')}
          style={{ left: `${String(tick.left)}%`, width: `${String(tick.width)}%` }}
        />
      ))}
      {todayLeft !== null && (
        <span
          className="absolute inset-y-0 z-[2] w-0.5 -translate-x-1/2 bg-danger"
          style={{ left: `${String(todayLeft)}%` }}
        />
      )}
    </div>
  )
}

/** Grille Gantt : en-tete temporel, lignes groupees par chantier, barres d'affectation, repere du jour. */
export function PlanningTimeline({
  groupes,
  zoom,
  periode,
  onSlotClick,
  today = new Date(),
}: PlanningTimelineProps) {
  const ticks = graduations(zoom, periode)
  const todayLeft = positionJour(today, periode)
  const minWidth = nombreDeJours(periode) * PX_PAR_JOUR[zoom]

  return (
    <div className="scrollbar-thin overflow-x-auto" role="region" aria-label="Diagramme des affectations">
      <div className="flex min-w-full flex-col" style={{ width: `calc(${String(minWidth)}px + 15rem)` }}>
        <div className="sticky top-0 z-[3] flex border-b border-border bg-surface-sunken/95 backdrop-blur">
          <div
            className={cn(
              LABEL_COL,
              'sticky left-0 z-[4] border-r border-border bg-surface-sunken px-3 py-2 text-xs font-medium text-muted-foreground',
            )}
          >
            Matériel
          </div>
          <div className="relative h-11 flex-1">
            {ticks.map((tick) => (
              <div
                key={tick.key}
                className={cn(
                  'absolute inset-y-0 flex flex-col justify-center overflow-hidden border-l border-border/60 px-1 text-2xs leading-tight',
                  tick.weekend ? 'text-muted-foreground/70' : 'text-muted-foreground',
                )}
                style={{ left: `${String(tick.left)}%`, width: `${String(tick.width)}%` }}
              >
                <span className="truncate font-medium capitalize">{tick.label}</span>
                {tick.sousLabel && <span className="truncate uppercase">{tick.sousLabel}</span>}
              </div>
            ))}
            {todayLeft !== null && (
              <span
                className="absolute bottom-0 -translate-x-1/2 rounded-t bg-danger px-1 text-2xs font-semibold text-danger-foreground"
                style={{ left: `${String(todayLeft)}%` }}
              >
                Auj.
              </span>
            )}
          </div>
        </div>
        {groupes.map((groupe) => (
          <section key={groupe.chantierId} aria-label={`Chantier ${groupe.chantierNom}`}>
            <div className="flex border-b border-border bg-muted/40">
              <h3
                className={cn(
                  LABEL_COL,
                  'sticky left-0 z-[2] flex items-center gap-2 bg-muted px-3 py-1.5 text-xs font-semibold',
                )}
              >
                <Building2 className="size-3.5 shrink-0 text-muted-foreground" aria-hidden />
                <span className="truncate">{groupe.chantierNom}</span>
                <span className="ml-auto tabular-nums text-muted-foreground">{groupe.lignes.length}</span>
              </h3>
              <div className="relative flex-1">
                <GridLines ticks={ticks} todayLeft={todayLeft} />
              </div>
            </div>
            {groupe.lignes.map((ligne) => (
              <div key={ligne.materielId} className="group flex border-b border-border last:border-b-0">
                <div
                  className={cn(
                    LABEL_COL,
                    'sticky left-0 z-[2] border-r border-border bg-surface px-3 py-1.5 group-hover:bg-surface-sunken',
                  )}
                >
                  <p className="truncate font-mono text-xs font-medium">{ligne.materielCode}</p>
                  <p className="truncate text-2xs text-muted-foreground">{ligne.materielDesignation}</p>
                </div>
                <div className="relative h-11 flex-1">
                  <GridLines ticks={ticks} todayLeft={todayLeft} />
                  {onSlotClick && (
                    <button
                      type="button"
                      className="absolute inset-0 cursor-copy focus-visible:bg-primary/5 focus-visible:ring-inset focus-visible:ring-offset-0"
                      aria-label={`Affecter ${ligne.materielCode} sur ce créneau`}
                      onClick={(event: MouseEvent<HTMLButtonElement>) => {
                        const rect = event.currentTarget.getBoundingClientRect()
                        const clavier = event.detail === 0 || rect.width === 0
                        const jour = clavier
                          ? todayLeft !== null
                            ? today
                            : periode.du
                          : jourAuPoint((event.clientX - rect.left) / rect.width, periode)
                        onSlotClick(ligne, groupe.chantierId, jour)
                      }}
                    />
                  )}
                  {ligne.affectations.map((affectation) => {
                    const position = positionBarre(affectation.dateDebut, finEffective(affectation), periode)
                    return position ? (
                      <AffectationBar key={affectation.id} affectation={affectation} position={position} />
                    ) : null
                  })}
                </div>
              </div>
            ))}
          </section>
        ))}
      </div>
    </div>
  )
}
