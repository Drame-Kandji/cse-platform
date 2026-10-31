import { Lock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { to } from '@/app/routes'
import { isApiError } from '@/shared/api'
import { useCurrentUser } from '@/shared/auth'
import { labelOf } from '@/shared/domain/labels'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { BesoinForm } from '../components/BesoinForm'
import { useModifierBesoin } from '../hooks/useBesoinMutations'
import { useBesoin } from '../hooks/useBesoins'
import { besoinDefaults } from '../schemas/besoin.schema'

/** Modification d'un besoin par son demandeur, uniquement tant qu'il est EN_ATTENTE (R05). */
export function Component() {
  const { t } = useTranslation('besoins')
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const user = useCurrentUser()
  const query = useBesoin(id)
  const modifier = useModifierBesoin(id)

  if (query.isPending) {
    return (
      <div className="mx-auto grid w-full max-w-3xl gap-4">
        <Skeleton className="h-8 w-72" />
        <Skeleton className="h-72 w-full" />
      </div>
    )
  }
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />

  const besoin = query.data
  const back = (
    <Button asChild variant="secondary">
      <Link to={to.besoinDetail(besoin.id)}>{t('edit.backToDetail')}</Link>
    </Button>
  )
  if (besoin.statut !== 'EN_ATTENTE' || besoin.demandeurId !== user.id) {
    return (
      <Card className="mx-auto max-w-xl">
        <EmptyState
          icon={<Lock aria-hidden />}
          title={t('edit.lockedTitle')}
          description={
            besoin.statut !== 'EN_ATTENTE'
              ? t('edit.lockedDescription', { statut: labelOf('statutBesoin', besoin.statut) })
              : t('edit.notOwner')
          }
          action={back}
        />
      </Card>
    )
  }

  return (
    <div className="mx-auto w-full max-w-3xl">
      <PageHeader
        title={t('form.editTitle', { reference: besoin.reference })}
      />
      <BesoinForm
        defaultValues={besoinDefaults(besoin)}
        submitLabel={t('form.submitEdit')}
        onCancel={() => {
          navigate(to.besoinDetail(besoin.id))
        }}
        onSubmit={async (payload) => {
          try {
            await modifier.mutateAsync({ ...payload, version: besoin.version })
            navigate(to.besoinDetail(besoin.id), { replace: true })
          } catch (error) {
            // R05 : le besoin a ete traite entre-temps -> on recharge pour afficher l'etat verrouille.
            if (isApiError(error) && error.status === 409) void query.refetch()
            throw error
          }
        }}
      />
    </div>
  )
}
