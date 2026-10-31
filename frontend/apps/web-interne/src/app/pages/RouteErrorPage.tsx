import { AlertOctagon, RefreshCw } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { isRouteErrorResponse, useRouteError } from 'react-router-dom'
import { Button } from '@/shared/ui/button'
import { ErrorState } from '@/shared/ui/error-state'
import { StatusPage } from '@/shared/ui/status-page'
import { Component as NotFoundPage } from './NotFoundPage'

function isChunkError(error: unknown): boolean {
  return (
    error instanceof Error &&
    /dynamically imported module|Importing a module script failed|Failed to fetch/i.test(error.message)
  )
}

/** Element d'erreur des routes : 404, nouvelle version deployee, ou erreur inattendue. */
export function RouteErrorPage() {
  const { t } = useTranslation()
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />
  const reload = () => {
    window.location.reload()
  }
  if (isChunkError(error)) {
    return (
      <StatusPage
        icon={<RefreshCw aria-hidden />}
        title={t('error.chunkTitle')}
        description={t('error.chunkDescription')}
        actions={<Button onClick={reload}>{t('actions.reload')}</Button>}
      />
    )
  }
  return (
    <StatusPage
      icon={<AlertOctagon aria-hidden />}
      title={t('error.boundaryTitle')}
      description={t('error.boundaryDescription')}
      actions={
        <>
          <Button
            variant="secondary"
            onClick={() => {
              window.location.assign('/')
            }}
          >
            {t('actions.goHome')}
          </Button>
          <Button onClick={reload}>{t('actions.reload')}</Button>
        </>
      }
    >
      {import.meta.env.DEV && <ErrorState error={error} size="compact" className="mt-6 max-w-lg" />}
    </StatusPage>
  )
}
