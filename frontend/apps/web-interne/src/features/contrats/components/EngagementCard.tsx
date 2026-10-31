import { useTranslation } from 'react-i18next'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Card, CardContent } from '@/shared/ui'
import type { StatutContrat } from '@/shared/domain/labels'
import type { EngagementContrat } from '../api/types'

interface Props {
  engagement: EngagementContrat | null | undefined
  statutContrat: StatutContrat
}

/**
 * Etat de l'engagement budgetaire (R06) : statut, montant, decideur, commentaire.
 * Le refus DAF (retour en BROUILLON) est signale dans la synthese du contrat.
 */
export function EngagementCard({ engagement }: Props) {
  const { t } = useTranslation('contrats')
  return (
    <Card>
      <CardContent className="grid gap-4">
        {engagement ? (
          <dl className="grid max-w-xl gap-3 text-sm">
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
