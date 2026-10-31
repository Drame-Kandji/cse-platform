import { CheckCircle2, FileSignature, Hourglass, Lock } from 'lucide-react'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { paths, to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import type { AppelOffre, Attribution } from '../api/types'
import { useContratLie } from '../hooks/useAppelsOffres'
import { ApprobationPanel } from './ApprobationPanel'
import { ProposerAttributionForm } from './ProposerAttributionForm'

function Resume({ ao, attribution }: { ao: AppelOffre; attribution: Attribution }) {
  return (
    <Card>
      <CardHeader title="Proposition d'attribution" />
      <CardContent className="grid gap-4">
        <dl className="grid gap-4 sm:grid-cols-3">
          <div>
            <dt className="text-xs text-muted-foreground">Fournisseur</dt>
            <dd className="text-sm font-medium">
              {ao.statut === 'PUBLIE'
                ? attribution.codeAnonyme
                : `${attribution.fournisseurNom} (${attribution.codeAnonyme})`}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Montant</dt>
            <dd className="text-sm font-semibold tabular-nums">{formatMoney(attribution.montant)}</dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Proposée par</dt>
            <dd className="text-sm">
              {attribution.proposeeParNom} · {formatDateTime(attribution.dateProposition)}
            </dd>
          </div>
        </dl>
        <div>
          <p className="text-xs text-muted-foreground">Justification</p>
          <p className="whitespace-pre-line text-sm">{attribution.justification}</p>
        </div>
      </CardContent>
    </Card>
  )
}

/** Onglet Attribution : proposition (RA) puis circuit de co-validation / approbation (R11). */
export function AttributionTab({ ao }: { ao: AppelOffre }) {
  const estRa = useHasRole(access.achatsGestion)
  const voitContrats = useHasRole(access.contrats)
  const contrat = useContratLie(ao, voitContrats && ao.statut === 'ATTRIBUE')
  if (ao.statut === 'PUBLIE') {
    return (
      <EmptyState
        icon={<Lock aria-hidden />}
        title="Attribution possible après la clôture"
      />
    )
  }
  const attribution = ao.attribution
  if (!attribution) {
    return estRa && ao.statut === 'CLOTURE' ? (
      <ProposerAttributionForm ao={ao} />
    ) : (
      <EmptyState
        icon={<Hourglass aria-hidden />}
        title="Aucune attribution proposée"
      />
    )
  }
  return (
    <div className="grid gap-4">
      {ao.statut === 'ATTRIBUE' && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <p className="flex flex-1 items-center gap-2 text-sm text-muted-foreground">
            <CheckCircle2 className="size-4 shrink-0 text-primary" aria-hidden />
            Attribution approuvée · contrat généré en brouillon (R03)
          </p>
          {voitContrats && (
            <Button asChild size="sm" variant="secondary">
              <Link to={contrat.data ? to.contratDetail(contrat.data.id) : paths.contrats}>
                <FileSignature aria-hidden />
                {contrat.data
                  ? contrat.data.numero
                    ? `Voir le contrat ${contrat.data.numero}`
                    : 'Voir le contrat'
                  : 'Voir les contrats'}
              </Link>
            </Button>
          )}
        </div>
      )}
      <Resume ao={ao} attribution={attribution} />
      <ApprobationPanel ao={ao} attribution={attribution} />
    </div>
  )
}
