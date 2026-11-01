import { LogOut, RotateCw, ServerCrash, ShieldAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router-dom'
import { useAuth } from '@/shared/auth/context'
import { Button } from '@/shared/ui/button'
import { LogoMark } from '@/shared/ui/logo'
import { Spinner } from '@/shared/ui/skeleton'
import { StatusPage } from '@/shared/ui/status-page'
import { estFournisseur } from '../access'
import { LoginPage } from './LoginPage'

/** Garde du portail : attente, erreur Keycloak, connexion, acces reserve aux fournisseurs, ou portail. */
export function AuthGate() {
  const { t } = useTranslation('portail')
  const { status, user, logout } = useAuth()
  if (status === 'loading') {
    return (
      <div className="grid min-h-dvh place-items-center bg-background">
        <div className="flex flex-col items-center gap-5">
          <LogoMark className="size-12" />
          <Spinner label={t('auth.loading')} />
        </div>
      </div>
    )
  }
  if (status === 'unauthenticated') return <LoginPage />
  if (status === 'error') {
    return (
      <StatusPage
        className="min-h-dvh"
        icon={<ServerCrash aria-hidden />}
        title={t('auth.errorTitle')}
        description={t('auth.errorDescription')}
        actions={
          <Button
            leftIcon={<RotateCw />}
            onClick={() => {
              window.location.reload()
            }}
          >
            {t('auth.retry')}
          </Button>
        }
      />
    )
  }
  if (!estFournisseur(user)) {
    return (
      <StatusPage
        className="min-h-dvh"
        icon={<ShieldAlert aria-hidden />}
        title={t('auth.reservedTitle')}
        description={t('auth.reservedDescription')}
        actions={
          <Button
            leftIcon={<LogOut />}
            onClick={() => {
              void logout()
            }}
          >
            {t('shell.logout')}
          </Button>
        }
      />
    )
  }
  return <Outlet />
}
