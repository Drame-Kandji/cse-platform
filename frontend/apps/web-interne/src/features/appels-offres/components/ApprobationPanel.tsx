import { Check, Info, X } from 'lucide-react'
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
    <li className="grid gap-2 rounded-lg border border-border p-4 sm:grid-cols-[1fr_auto] sm:items-start">
      <div className="grid gap-1">
        <p className="flex flex-wrap items-center gap-2 text-sm font-medium">
          {titre}
          <StatusBadge kind="statutApprobation" value={approbation.statut} />
        </p>
        <p className="text-xs text-muted-foreground">{responsable}</p>
        {approbation.parNom && (
          <p className="text-sm text-muted-foreground">
            {approbation.statut === 'REFUSEE' ? 'Refusée' : 'Décidée'} par {approbation.parNom}
            {approbation.date && ` le ${formatDateTime(approbation.date)}`}
          </p>
        )}
        {approbation.commentaire && (
          <p className="text-sm italic text-foreground">« {approbation.commentaire} »</p>
        )}
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
        variant="secondary"
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
      <div className="flex items-start gap-3 rounded-lg border border-info/30 bg-info/10 px-4 py-3 text-sm text-info-emphasis">
        <Info className="mt-0.5 size-4 shrink-0" aria-hidden />
        <p>
          {dg
            ? `Montant de ${formatMoney(attribution.montant)} supérieur au seuil de délégation : l'attribution doit être co-validée par le Responsable Matériel puis approuvée par la Direction Générale (R11).`
            : `Montant de ${formatMoney(attribution.montant)} dans le seuil de délégation : seule la co-validation du Responsable Matériel est requise, sans approbation de la Direction Générale (R11).`}
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
            responsable="Direction Générale — requise au-delà du seuil de délégation"
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
          <li className="rounded-lg border border-dashed border-border p-4 text-sm text-muted-foreground">
            Approbation de la Direction Générale : non requise pour ce montant.
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
