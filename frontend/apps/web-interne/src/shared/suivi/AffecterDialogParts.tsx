import { AlertTriangle, CalendarX2 } from 'lucide-react'
import type { ReactNode } from 'react'
import { getErrorMessage, isApiError } from '@/shared/api/errors'
import { labelOf, type EtatMateriel, type Tone } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { softToneClasses } from '@/shared/ui/tone'
import type { MaterielAffectable } from './AffecterDialog'

/** Encadre d'information ou d'erreur dans un formulaire (role alert si erreur). */
export function InlineAlert({
  tone,
  title,
  children,
  icon,
}: {
  tone: Extract<Tone, 'warning' | 'danger' | 'info'>
  title: ReactNode
  children?: ReactNode
  icon?: ReactNode
}) {
  return (
    <div
      role={tone === 'info' ? 'status' : 'alert'}
      className={cn('flex gap-3 rounded-lg px-3.5 py-3 text-sm ring-1 ring-inset', softToneClasses[tone])}
    >
      <span className="mt-0.5 shrink-0 [&_svg]:size-4" aria-hidden>
        {icon ?? <AlertTriangle />}
      </span>
      <div className="grid gap-0.5">
        <p className="font-medium">{title}</p>
        {children && <div className="text-foreground/80">{children}</div>}
      </div>
    </div>
  )
}

/** Rappel du materiel affecte et de son etat courant. */
export function MaterielSummary({
  materiel,
  etat,
  loading,
}: {
  materiel: MaterielAffectable
  etat: EtatMateriel | undefined
  loading: boolean
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg bg-muted/60 px-3.5 py-3">
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{materiel.designation}</p>
        <p className="font-mono text-xs text-muted-foreground">{materiel.code}</p>
      </div>
      {loading ? <Skeleton className="h-6 w-24" /> : etat && <StatusBadge kind="etatMateriel" value={etat} />}
    </div>
  )
}

/** Alerte R04 (materiel non disponible) et erreurs serveur (conflit de periode, regle metier). */
export function AffectationAlert({
  nonDisponible,
  etat,
  error,
}: {
  nonDisponible: boolean
  etat: EtatMateriel | undefined
  error: unknown
}) {
  if (nonDisponible && etat) {
    return (
      <InlineAlert tone="warning" title="Affectation impossible">
        Ce matériel est « {labelOf('etatMateriel', etat)} ». Seul un matériel disponible peut être affecté.
      </InlineAlert>
    )
  }
  if (!error) return null
  if (isApiError(error) && error.code === 'CONFLIT_AFFECTATION') {
    return (
      <InlineAlert tone="danger" title="Période déjà occupée" icon={<CalendarX2 />}>
        {error.detail ?? 'Le matériel est déjà affecté sur une partie de cette période.'} Choisissez
        d&apos;autres dates.
      </InlineAlert>
    )
  }
  const title =
    isApiError(error) && error.code === 'MATERIEL_NON_DISPONIBLE'
      ? "Le matériel n'est plus disponible"
      : "L'affectation a été refusée"
  return (
    <InlineAlert tone="danger" title={title}>
      {getErrorMessage(error)}
    </InlineAlert>
  )
}
