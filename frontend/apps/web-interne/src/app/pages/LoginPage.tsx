import { useTranslation } from 'react-i18next'
import { useAuth } from '@/shared/auth/context'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { DevRolePicker } from './DevRolePicker'
import { LoginLayout } from './LoginLayout'

/** Page de connexion : redirection Keycloak (PKCE) ou selecteur de role en mode dev. */
export function LoginPage() {
  const { t } = useTranslation('auth')
  const { login, devSignIn } = useAuth()
  return (
    <LoginLayout wide={Boolean(devSignIn)}>
      {devSignIn ? (
        <DevRolePicker onSelect={devSignIn} />
      ) : (
        <Card className="grid gap-6 p-6 sm:p-8">
          <h1 className="text-2xl font-semibold tracking-tight">{t('login.title')}</h1>
          <Button
            size="lg"
            className="w-full"
            onClick={() => {
              void login()
            }}
          >
            {t('login.submit')}
          </Button>
        </Card>
      )}
    </LoginLayout>
  )
}
