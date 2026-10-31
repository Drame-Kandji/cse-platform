import { useTranslation } from 'react-i18next'
import { Spinner } from '@/shared/ui/skeleton'
import { LogoMark } from '../layout/Logo'

/** Ecran d'attente pendant la verification de session. */
export function SplashScreen() {
  const { t } = useTranslation('auth')
  return (
    <div className="grid min-h-dvh place-items-center bg-background">
      <div className="flex flex-col items-center gap-5">
        <LogoMark className="size-12" />
        <Spinner label={t('loading')} />
      </div>
    </div>
  )
}
