import { useTranslation } from 'react-i18next'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { InfoList } from '@/components/InfoList'
import { ResultatBadge } from '@/domain/badges'
import { resultatOffre } from '@/domain/offres'
import type { OffrePortail } from '@/domain/types'

/** Offre deja deposee par le fournisseur. */
export function MonOffreCard({ offre }: { offre: OffrePortail }) {
  const { t } = useTranslation('portail')
  return (
    <Card>
      <CardHeader title={t('ao.monOffre')} action={<ResultatBadge resultat={resultatOffre(offre)} />} />
      <CardContent className="grid gap-4 pt-4">
        <InfoList
          items={[
            { label: t('ao.dialog.montant'), value: formatMoney(offre.montantPropose) },
            { label: t('ao.dialog.delai'), value: t('ao.delaiJours', { n: offre.delaiLivraisonJours }) },
            {
              label: t('ao.dialog.garantie'),
              value: t('ao.garantieMois', { n: offre.garantieMois ?? 0 }),
              hidden: offre.garantieMois == null,
            },
            { label: t('ao.columns.depot'), value: formatDateTime(offre.dateDepot) },
          ]}
        />
        {offre.description && (
          <p className="whitespace-pre-line text-sm text-muted-foreground">{offre.description}</p>
        )}
      </CardContent>
    </Card>
  )
}
