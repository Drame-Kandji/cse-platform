import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { cn } from '@/shared/lib/cn'
import { LogoMark } from '@/shared/ui/logo'

/** Ecrans hors session (connexion, inscription) : marque discrete, contenu centre. */
export function PublicLayout({ children, wide = false }: { children: ReactNode; wide?: boolean }) {
  const { t } = useTranslation('portail')
  return (
    <div className="flex min-h-dvh flex-col bg-background px-4 py-6 sm:px-10">
      <header className="flex items-center justify-between">
        <span className="flex items-center gap-2.5">
          <LogoMark />
          <span className="text-sm font-semibold">{t('app.name')}</span>
        </span>
        <ThemeToggle />
      </header>
      <main
        className={cn(
          'mx-auto flex w-full flex-1 flex-col justify-center py-10',
          wide ? 'max-w-2xl' : 'max-w-sm',
        )}
      >
        {children}
      </main>
    </div>
  )
}
