import { AlertTriangle, Landmark } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Card, CardContent, CardHeader, StatusBadge } from '@/shared/ui'
import type { StatutContrat } from '@/shared/domain/labels'
import type { EngagementContrat } from '../api/types'

interface Props {
  engagement: EngagementContrat | null | undefined
  statutContrat: StatutContrat
}

/**
 * Etat de l'engagement budgetaire (R06) : statut, montant, decideur, commentaire.
 * Un refus DAF renvoie le contrat en BROUILLON : un bandeau invite a corriger puis re-signer.
 */
export function EngagementCard({ engagement, statutContrat }: Props) {
  const { t } = useTranslation('contrats')
  const refuse = statutContrat === 'BROUILLON' && engagement?.statut === 'REFUSEE'
  return (
    <Card>
      <CardHeader
        icon={<Landmark aria-hidden />}
        title={t('engagement.title')}
        description={t('engagement.description')}
        action={engagement ? <StatusBadge kind="statutApprobation" value={engagement.statut} /> : null}
      />
      <CardContent className="grid gap-4">
        {refuse && (
          <div
            role="status"
            className="flex gap-3 rounded-lg border border-danger/30 bg-danger/10 p-3 text-sm text-danger-emphasis"
          >
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
            <div className="grid gap-1">
              <p className="font-semibold">{t('engagement.refuse')}</p>
            </div>
          </div>
        )}
        {engagement ? (
          <dl className="grid gap-3 text-sm">
            <div className="flex justify-between gap-3">
              <dt className="text-muted-foreground">{t('fields.montant')}</dt>
              <dd className="font-semibold tabular-nums">{formatMoney(engagement.montant)}</dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-muted-foreground">{t('engagement.demande')}</dt>
              <dd className="tabular-nums">{formatDateTime(engagement.dateDemande)}</dd>
            </div>
            {engagement.dateDecision && (
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">{t('engagement.decision')}</dt>
                <dd className="tabular-nums">{formatDateTime(engagement.dateDecision)}</dd>
              </div>
            )}
            {engagement.parNom && (
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">{t('engagement.par')}</dt>
                <dd>{engagement.parNom}</dd>
              </div>
            )}
            {engagement.commentaire && (
              <div>
                <dt className="sr-only">Commentaire</dt>
                <dd className="rounded-lg bg-surface-sunken p-3 text-muted-foreground">
                  {engagement.commentaire}
                </dd>
              </div>
            )}
          </dl>
        ) : (
          <p className="text-sm text-muted-foreground">{t('engagement.none')}</p>
        )}
      </CardContent>
    </Card>
  )
}
