import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { CountTabs } from '@/shared/ui/count-tabs'
import { PageHeader } from '@/shared/ui/page-header'
import { useAppelsOffres, useMesOffres } from '@/api/queries'
import { MesOffresTable } from '../components/MesOffresTable'
import { OuvertsTable } from '../components/OuvertsTable'

type Onglet = 'ouverts' | 'mes-offres'

/** Appels d'offres : onglets « Ouverts » et « Mes offres » (synchronises avec l'URL). */
export function Component() {
  const { t } = useTranslation('portail')
  const [params, setParams] = useSearchParams()
  const onglet: Onglet = params.get('onglet') === 'mes-offres' ? 'mes-offres' : 'ouverts'
  const ouverts = useAppelsOffres()
  const offres = useMesOffres()
  return (
    <>
      <PageHeader title={t('nav.appelsOffres')} />
      <CountTabs<Onglet>
        label={t('nav.appelsOffres')}
        value={onglet}
        loading={ouverts.isPending || offres.isPending}
        items={[
          { value: 'ouverts', label: t('ao.ouverts'), count: ouverts.data?.totalElements },
          { value: 'mes-offres', label: t('ao.mesOffres'), count: offres.data?.totalElements },
        ]}
        onChange={(value) => {
          if (value) setParams(value === 'ouverts' ? {} : { onglet: value }, { replace: true })
        }}
      />
      {onglet === 'ouverts' ? <OuvertsTable query={ouverts} /> : <MesOffresTable query={offres} />}
    </>
  )
}
