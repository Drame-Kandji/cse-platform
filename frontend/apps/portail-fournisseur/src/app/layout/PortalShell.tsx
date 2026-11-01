import { useTranslation } from 'react-i18next'
import { Outlet, useNavigation } from 'react-router-dom'
import { ProgressBar } from '@/shared/ui/skeleton'
import { Topbar } from './Topbar'

/** Coquille du portail : barre superieure avec navigation, contenu centre. */
export function PortalShell() {
  const { t } = useTranslation('portail')
  const navigation = useNavigation()
  return (
    <>
      <a
        href="#contenu"
        className="sr-only z-50 rounded-lg bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
      >
        {t('shell.skip')}
      </a>
      <ProgressBar active={navigation.state === 'loading'} className="fixed inset-x-0 top-0 z-50" />
      <div className="flex min-h-dvh flex-col bg-background">
        <Topbar />
        <main
          id="contenu"
          tabIndex={-1}
          className="flex-1 px-4 pb-20 pt-6 focus:outline-none sm:px-6 sm:pt-8"
        >
          <div className="mx-auto w-full max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </>
  )
}
