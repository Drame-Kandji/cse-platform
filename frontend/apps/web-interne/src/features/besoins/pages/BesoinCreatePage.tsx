import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { paths, to } from '@/app/routes'
import { useCurrentUser } from '@/shared/auth'
import { PageHeader } from '@/shared/ui/page-header'
import { BesoinForm } from '../components/BesoinForm'
import { useCreerBesoin } from '../hooks/useBesoinMutations'
import { besoinDefaults } from '../schemas/besoin.schema'

/** Expression d'un nouveau besoin (CHEF sur ses chantiers, RM). */
export function Component() {
  const { t } = useTranslation('besoins')
  const navigate = useNavigate()
  const user = useCurrentUser()
  const creer = useCreerBesoin()
  const chantierParDefaut = user.chantierIds.length === 1 ? (user.chantierIds[0] ?? '') : ''
  return (
    <div className="mx-auto w-full max-w-3xl">
      <PageHeader title={t('form.createTitle')} />
      <BesoinForm
        defaultValues={besoinDefaults(undefined, chantierParDefaut)}
        submitLabel={t('form.submitCreate')}
        onCancel={() => {
          navigate(paths.besoins)
        }}
        onSubmit={async (payload) => {
          const besoin = await creer.mutateAsync(payload)
          navigate(to.besoinDetail(besoin.id), { replace: true })
        }}
      />
    </div>
  )
}
