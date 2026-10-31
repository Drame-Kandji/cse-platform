import { ArrowRight, LockKeyhole, ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/shared/auth/context'
import { Button } from '@/shared/ui/button'
import { DevRolePicker } from './DevRolePicker'
import { LoginLayout } from './LoginLayout'

/** Page de connexion : redirection Keycloak (PKCE) ou selecteur de role en mode dev. */
export function LoginPage() {
  const { t } = useTranslation('auth')
  const { login, devSignIn } = useAuth()
  return (
    <LoginLayout>
      {devSignIn ? (
        <DevRolePicker onSelect={devSignIn} />
      ) : (
        <div className="mx-auto w-full max-w-sm">
          <div className="grid size-12 place-items-center rounded-xl border border-border bg-surface shadow-card">
            <LockKeyhole className="size-5 text-primary-emphasis" aria-hidden />
          </div>
          <h2 className="mt-6 text-2xl font-semibold tracking-tight">{t('login.title')}</h2>
          <p className="mt-2 text-sm text-muted-foreground">{t('login.subtitle')}</p>
          <Button
            size="lg"
            className="mt-8 w-full"
            rightIcon={<ArrowRight />}
            onClick={() => {
              void login()
            }}
          >
            {t('login.submit')}
          </Button>
          <p className="mt-6 flex items-center gap-2 text-xs text-muted-foreground">
            <ShieldCheck className="size-4 text-success-emphasis" aria-hidden />
            {t('login.secure')}
          </p>
        </div>
      )}
    </LoginLayout>
  )
}
