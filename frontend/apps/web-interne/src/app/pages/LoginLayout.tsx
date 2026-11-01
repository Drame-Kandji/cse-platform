import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { LogoMark } from '@/shared/ui/logo'

/** Mise en page des ecrans de connexion : fond clair, marque discrete, contenu centre. */
export function LoginLayout({ children, wide = false }: { children: ReactNode; wide?: boolean }) {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation()
  return (
    <div className="flex min-h-dvh flex-col bg-background px-5 py-6 sm:px-10">
      <header className="flex items-center justify-between">
        <span className="flex items-center gap-2.5">
          <LogoMark />
          <span className="text-sm font-semibold">{tc('app.name')}</span>
        </span>
        <ThemeToggle />
      </header>
      <main
        className={`mx-auto flex w-full flex-1 flex-col justify-center py-10 ${wide ? 'max-w-xl' : 'max-w-sm'}`}
      >
        {children}
      </main>
      <p className="text-center text-xs text-muted-foreground">{t('login.footer')}</p>
    </div>
  )
}
