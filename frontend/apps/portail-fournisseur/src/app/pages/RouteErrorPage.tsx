import { AlertOctagon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { isRouteErrorResponse, useRouteError } from 'react-router-dom'
import { Button } from '@/shared/ui/button'
import { StatusPage } from '@/shared/ui/status-page'
import { Component as NotFoundPage } from './NotFoundPage'

/** Erreur de route : 404 ou erreur inattendue (rechargement propose). */
export function RouteErrorPage() {
  const { t } = useTranslation()
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />
  return (
    <StatusPage
      icon={<AlertOctagon aria-hidden />}
      title={t('error.boundaryTitle')}
      description={t('error.boundaryDescription')}
      actions={
        <Button
          onClick={() => {
            window.location.reload()
          }}
        >
          {t('actions.reload')}
        </Button>
      }
    />
  )
}
