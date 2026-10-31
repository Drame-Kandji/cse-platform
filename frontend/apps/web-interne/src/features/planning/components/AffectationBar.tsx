import { ArrowUpRight } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { labelOf } from '@/shared/domain/labels'
import type { Affectation } from '@/shared/domain/types'
import { cn } from '@/shared/lib/cn'
import { formatDate, formatDateTime } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/ui/popover'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { PositionBarre } from '../lib/layout'
import { finEffective } from '../lib/rows'
import { BAR_CLASSES } from '../lib/styles'

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid grid-cols-[7rem_1fr] gap-2 text-xs">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="tabular-nums">{children}</dd>
    </div>
  )
}

/** Barre d'affectation positionnee en pourcentage ; un clic ouvre le detail. */
export function AffectationBar({
  affectation,
  position,
}: {
  affectation: Affectation
  position: PositionBarre
}) {
  const fin = finEffective(affectation)
  const periode = `${formatDate(affectation.dateDebut)} au ${formatDate(fin)}`
  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          data-testid="planning-bar"
          aria-label={`${affectation.materielCode} sur ${affectation.chantierNom}, ${periode}, ${labelOf('statutAffectation', affectation.statut)}`}
          className={cn(
            'absolute inset-y-1.5 z-[1] flex items-center overflow-hidden rounded-md px-1.5 text-2xs font-medium transition-colors',
            BAR_CLASSES[affectation.statut],
            position.coupeeAvant && 'rounded-l-none',
            position.coupeeApres && 'rounded-r-none',
          )}
          style={{ left: `${String(position.left)}%`, width: `max(${String(position.width)}%, 6px)` }}
        >
          <span className="truncate">
            {formatDate(affectation.dateDebut, 'd MMM')} – {formatDate(fin, 'd MMM')}
          </span>
        </button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-4" align="start">
        <div className="grid gap-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">{affectation.materielDesignation}</p>
              <p className="font-mono text-xs text-muted-foreground">{affectation.materielCode}</p>
            </div>
            <StatusBadge kind="statutAffectation" value={affectation.statut} size="sm" />
          </div>
          <dl className="grid gap-1.5">
            <Detail label="Chantier">{affectation.chantierNom}</Detail>
            <Detail label="Période prévue">
              {formatDate(affectation.dateDebut)} → {formatDate(affectation.dateFinPrevue)}
            </Detail>
            {affectation.dateFinReelle && (
              <Detail label="Fin réelle">{formatDate(affectation.dateFinReelle)}</Detail>
            )}
            {affectation.dateReception && (
              <Detail label="Réception">{formatDateTime(affectation.dateReception)}</Detail>
            )}
            <Detail label="Catégorie">{labelOf('categorieMateriel', affectation.categorie)}</Detail>
          </dl>
          <Button variant="secondary" size="sm" rightIcon={<ArrowUpRight />} asChild>
            <Link to={to.materielDetail(affectation.materielId)}>Ouvrir la fiche engin</Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
