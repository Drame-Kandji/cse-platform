import { Check, X } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { access } from '@/app/access'
import { useHasRole } from '@/shared/auth'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Approbation, AppelOffre, Attribution } from '../api/types'
import { useDecisionAttribution } from '../hooks/useAppelOffreActions'
import { DecisionDialog } from './DecisionDialog'

interface EtapeProps {
  titre: string
  responsable: string
  approbation: Approbation
  actions?: ReactNode
}

function EtapeApprobation({ titre, responsable, approbation, actions }: EtapeProps) {
  return (
    <li className="grid gap-2 rounded-xl border border-border bg-surface p-4 sm:grid-cols-[1fr_auto] sm:items-start">
      <div className="grid gap-1">
        <p className="flex flex-wrap items-center gap-2 text-sm font-medium">
          {titre}
          <StatusBadge kind="statutApprobation" value={approbation.statut} />
        </p>
        <p className="text-xs text-muted-foreground">
          {responsable}
          {approbation.parNom && ` · ${approbation.parNom}`}
          {approbation.date && ` · ${formatDateTime(approbation.date)}`}
        </p>
        {approbation.commentaire && <p className="text-sm text-foreground">{approbation.commentaire}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </li>
  )
}

function Actions({ label, onDecide }: { label: string; onDecide: (approuve: boolean) => void }) {
  return (
    <>
      <Button
        size="sm"
        leftIcon={<Check />}
        onClick={() => {
          onDecide(true)
        }}
      >
        {label}
      </Button>
      <Button
        size="sm"
        variant="ghost"
        leftIcon={<X />}
        onClick={() => {
          onDecide(false)
        }}
      >
        Refuser
      </Button>
    </>
  )
}

/** Circuit d'approbation R11 : co-validation RM, puis approbation DG au-dela du seuil. */
export function ApprobationPanel({ ao, attribution }: { ao: AppelOffre; attribution: Attribution }) {
  const estRm = useHasRole(access.attributionCoValidation)
  const estDg = useHasRole(access.attributionApprobation)
  const coValider = useDecisionAttribution(ao.id, 'coValidation')
  const approuverDg = useDecisionAttribution(ao.id, 'approbationDg')
  const [decision, setDecision] = useState<{ niveau: 'rm' | 'dg'; approuve: boolean } | null>(null)
  const dg = attribution.approbationDg
  const mutation = decision?.niveau === 'dg' ? approuverDg : coValider

  return (
    <div className="grid gap-3">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-base font-semibold">Circuit d&apos;approbation</h3>
        <p className="text-right text-xs text-muted-foreground">
          {dg
            ? `${formatMoney(attribution.montant)} : supérieur au seuil de délégation (R11)`
            : `${formatMoney(attribution.montant)} : dans le seuil, sans approbation de la Direction Générale (R11)`}
        </p>
      </div>
      <ol className="grid gap-3" aria-label="Circuit d'approbation">
        <EtapeApprobation
          titre="Co-validation technique"
          responsable="Responsable Matériel"
          approbation={attribution.coValidationMateriel}
          actions={
            estRm && attribution.coValidationMateriel.statut === 'EN_ATTENTE' ? (
              <Actions
                label="Co-valider"
                onDecide={(approuve) => {
                  setDecision({ niveau: 'rm', approuve })
                }}
              />
            ) : undefined
          }
        />
        {dg ? (
          <EtapeApprobation
            titre="Approbation de la Direction Générale"
            responsable="Direction Générale"
            approbation={dg}
            actions={
              estDg && dg.statut === 'EN_ATTENTE' ? (
                <Actions
                  label="Approuver"
                  onDecide={(approuve) => {
                    setDecision({ niveau: 'dg', approuve })
                  }}
                />
              ) : undefined
            }
          />
        ) : (
          <li className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
            Approbation de la Direction Générale : non requise
          </li>
        )}
      </ol>
      <DecisionDialog
        approuve={decision?.approuve ?? null}
        titre={decision?.niveau === 'dg' ? 'approbation DG' : 'co-validation'}
        pending={mutation.isPending}
        onClose={() => {
          setDecision(null)
        }}
        onSubmit={(body) => mutation.mutateAsync(body)}
      />
    </div>
  )
}
