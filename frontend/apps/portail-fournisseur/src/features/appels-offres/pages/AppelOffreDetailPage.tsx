import { ArrowLeft, SearchX, Send } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { useAppelOffre, useAppelsOffres, useMesOffres } from '@/api/queries'
import { paths } from '@/app/routes'
import { useNow } from '@/components/use-now'
import { estIntrouvable } from '@/domain/errors'
import { estInvite, peutDeposer } from '@/domain/offres'
import { AppelOffreInfos } from '../components/AppelOffreInfos'
import { DeposerOffreDialog } from '../components/DeposerOffreDialog'
import { MonOffreCard } from '../components/MonOffreCard'

/**
 * Detail d'un appel d'offres. Le detail est lu sur `/portail/appels-offres/{id}` ; a defaut (route
 * absente, 404) on retombe sur la liste des ouverts, et l'offre deposee vient de « Mes offres ».
 */
export function Component() {
  const { t } = useTranslation('portail')
  const { id = '' } = useParams()
  const now = useNow()
  const [open, setOpen] = useState(false)
  const detail = useAppelOffre(id)
  const liste = useAppelsOffres()
  const offres = useMesOffres()

  const ao = detail.data ?? liste.data?.content.find((a) => a.id === id)
  const offre = offres.data?.content.find(
    (o) => o.appelOffreId === id || (ao?.monOffreId && o.id === ao.monOffreId),
  )

  if (!ao && !offre && (detail.isPending || liste.isPending || offres.isPending)) {
    return (
      <div className="grid gap-4">
        <Skeleton className="h-9 w-2/3" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }
  if (!ao && !offre) {
    if (detail.error && !estIntrouvable(detail.error)) {
      return <ErrorState error={detail.error} onRetry={() => void detail.refetch()} />
    }
    return (
      <EmptyState
        icon={<SearchX aria-hidden />}
        title={t('ao.introuvable')}
        action={
          <Button asChild variant="secondary" leftIcon={<ArrowLeft />}>
            <Link to={paths.appelsOffres}>{t('nav.appelsOffres')}</Link>
          </Button>
        }
      />
    )
  }

  const depot = ao && !offre && peutDeposer(ao, now)
  return (
    <>
      <PageHeader
        title={ao?.objet ?? offre?.appelOffreObjet}
        subtitle={ao?.reference ?? offre?.appelOffreReference}
        meta={
          ao &&
          estInvite(ao) && (
            <Badge tone="primary" size="sm">
              {t('ao.invite')}
            </Badge>
          )
        }
        actions={
          depot && (
            <Button
              leftIcon={<Send />}
              onClick={() => {
                setOpen(true)
              }}
            >
              {t('ao.deposer')}
            </Button>
          )
        }
      />
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="grid content-start gap-4 lg:col-span-2">
          {offre && <MonOffreCard offre={offre} />}
          {ao && (
            <Card>
              <CardHeader title={t('ao.description')} />
              <CardContent className="grid gap-4 pt-4 text-sm">
                <p className="whitespace-pre-line">{ao.description ?? ao.objet}</p>
                {ao.specifications && (
                  <div className="grid gap-1">
                    <p className="font-medium">{t('ao.specifications')}</p>
                    <p className="whitespace-pre-line text-muted-foreground">{ao.specifications}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </div>
        {ao && <AppelOffreInfos ao={ao} />}
      </div>
      {ao && depot && <DeposerOffreDialog appelOffre={ao} open={open} onOpenChange={setOpen} />}
    </>
  )
}
