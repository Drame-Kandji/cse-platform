import { RotateCw, ServerCrash } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router-dom'
import { useAuth } from '@/shared/auth/context'
import { Button } from '@/shared/ui/button'
import { StatusPage } from '@/shared/ui/status-page'
import { LoginPage } from './LoginPage'
import { SplashScreen } from './SplashScreen'

/** Garde d'authentification : attente, erreur Keycloak, connexion, ou application. */
export function AuthGate() {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation()
  const { status } = useAuth()
  if (status === 'loading') return <SplashScreen />
  if (status === 'unauthenticated') return <LoginPage />
  if (status === 'error') {
    return (
      <StatusPage
        className="min-h-dvh"
        icon={<ServerCrash aria-hidden />}
        title={t('error.title')}
        description={t('error.description')}
        actions={
          <Button
            leftIcon={<RotateCw />}
            onClick={() => {
              window.location.reload()
            }}
          >
            {tc('actions.retry')}
          </Button>
        }
      />
    )
  }
  return <Outlet />
}
