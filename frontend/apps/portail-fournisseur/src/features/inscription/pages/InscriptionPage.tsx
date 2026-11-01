import { CheckCircle2 } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { paths } from '@/app/routes'
import { PublicLayout } from '@/components/PublicLayout'
import type { InscriptionRecue } from '@/domain/types'
import { InscriptionForm } from '../components/InscriptionForm'

/** Inscription publique d'un fournisseur (sans connexion). */
export function Component() {
  const { t } = useTranslation('portail')
  const [recue, setRecue] = useState<InscriptionRecue | null>(null)
  if (recue) {
    return (
      <PublicLayout>
        <Card className="grid justify-items-center gap-4 p-6 text-center sm:p-8">
          <CheckCircle2 className="size-10 text-primary" aria-hidden />
          <h1 className="text-xl font-semibold tracking-tight">{t('inscription.successTitle')}</h1>
          <p className="text-sm text-muted-foreground">{t('inscription.successText')}</p>
          <Button asChild variant="secondary" className="mt-2">
            <Link to={paths.accueil}>{t('inscription.backToLogin')}</Link>
          </Button>
        </Card>
      </PublicLayout>
    )
  }
  return (
    <PublicLayout wide>
      <Card className="grid gap-6 p-5 sm:p-8">
        <h1 className="text-2xl font-semibold tracking-tight">{t('inscription.title')}</h1>
        <InscriptionForm onDone={setRecue} />
      </Card>
      <p className="mt-6 text-center text-sm text-muted-foreground">
        {t('inscription.already')}{' '}
        <Link to={paths.accueil} className="font-medium text-primary-emphasis hover:underline">
          {t('auth.login')}
        </Link>
      </p>
    </PublicLayout>
  )
}
