import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { useAuth } from '@/shared/auth/context'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { PublicLayout } from '@/components/PublicLayout'
import { paths } from '../routes'

/** Connexion (redirection Keycloak PKCE) et lien vers l'inscription publique. */
export function LoginPage() {
  const { t } = useTranslation('portail')
  const { login } = useAuth()
  return (
    <PublicLayout>
      <Card className="grid gap-6 p-6 sm:p-8">
        <h1 className="text-2xl font-semibold tracking-tight">{t('auth.title')}</h1>
        <Button
          size="lg"
          className="w-full"
          onClick={() => {
            void login()
          }}
        >
          {t('auth.login')}
        </Button>
      </Card>
      <p className="mt-6 text-center text-sm text-muted-foreground">
        {t('auth.noAccount')}{' '}
        <Link to={paths.inscription} className="font-medium text-primary-emphasis hover:underline">
          {t('auth.register')}
        </Link>
      </p>
    </PublicLayout>
  )
}
