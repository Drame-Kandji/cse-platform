import { useTranslation } from 'react-i18next'
import { labelOf } from '@/shared/domain/labels'
import { formatDate, formatDateTime, formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { Countdown } from '@/components/Countdown'
import { InfoList } from '@/components/InfoList'
import type { AppelOffrePortail } from '@/domain/types'

/** Faits cles et ponderation des criteres d'un appel d'offres. */
export function AppelOffreInfos({ ao }: { ao: AppelOffrePortail }) {
  const { t } = useTranslation('portail')
  const criteres = [...ao.criteres].sort((a, b) => b.poids - a.poids)
  return (
    <div className="grid content-start gap-4">
      <Card>
        <CardContent className="pt-5">
          <InfoList
            items={[
              {
                label: t('ao.columns.dateLimite'),
                value: (
                  <span className="grid justify-items-end gap-0.5">
                    {formatDateTime(ao.dateLimite)}
                    <Countdown dateLimite={ao.dateLimite} />
                  </span>
                ),
              },
              { label: t('ao.columns.categorie'), value: labelOf('categorieMateriel', ao.categorie) },
              { label: t('ao.quantiteLabel'), value: formatNumber(ao.quantite) },
              { label: t('ao.procedure'), value: labelOf('typeAppelOffre', ao.type) },
              { label: t('ao.chantier'), value: ao.chantierNom, hidden: !ao.chantierNom },
              { label: t('ao.publie'), value: formatDate(ao.datePublication) },
            ]}
          />
        </CardContent>
      </Card>
      {criteres.length > 0 && (
        <Card>
          <CardHeader title={t('ao.criteres')} />
          <CardContent className="pt-4">
            <InfoList
              items={criteres.map((c) => ({
                label: labelOf('critereEvaluation', c.code),
                value: `${c.poids} %`,
              }))}
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
