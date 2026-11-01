import { useTranslation } from 'react-i18next'
import { formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { Card, CardContent } from '@/shared/ui/card'
import { InfoList } from '@/components/InfoList'
import type { Contrat } from '@/domain/types'

/** Faits cles d'un contrat. */
export function ContratInfos({ contrat }: { contrat: Contrat }) {
  const { t } = useTranslation('portail')
  return (
    <Card>
      <CardContent className="pt-5">
        <InfoList
          items={[
            { label: t('contrats.columns.montant'), value: formatMoney(contrat.montant) },
            {
              label: t('contrats.infos.designation'),
              value: contrat.designation,
              hidden: contrat.designation === contrat.objet,
            },
            { label: t('ao.quantiteLabel'), value: formatNumber(contrat.quantite) },
            {
              label: t('contrats.infos.delai'),
              value: t('ao.delaiJours', { n: contrat.delaiLivraisonJours }),
            },
            {
              label: t('contrats.columns.livraison'),
              value: formatDate(contrat.dateLivraisonPrevue),
              hidden: !contrat.dateLivraisonPrevue,
            },
            { label: t('ao.chantier'), value: contrat.chantierNom, hidden: !contrat.chantierNom },
            {
              label: t('contrats.infos.signe'),
              value: formatDate(contrat.signatureFournisseur?.date ?? contrat.dateSignature),
              hidden: !contrat.signatureFournisseur && !contrat.dateSignature,
            },
            {
              label: t('contrats.infos.expedie'),
              value: formatDate(contrat.dateExpedition),
              hidden: !contrat.dateExpedition,
            },
            {
              label: t('contrats.infos.livre'),
              value: formatDate(contrat.dateLivraisonReelle),
              hidden: !contrat.dateLivraisonReelle,
            },
            {
              label: t('contrats.infos.bl'),
              value: contrat.numeroBonLivraison,
              hidden: !contrat.numeroBonLivraison,
            },
            {
              label: t('contrats.infos.penalites'),
              value: formatMoney(contrat.penalitesRetard),
              hidden: !contrat.penalitesRetard,
            },
          ]}
        />
      </CardContent>
    </Card>
  )
}
