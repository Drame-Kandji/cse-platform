import { Building2, ClipboardCheck, Wallet, type LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ThemeToggle } from '../layout/ThemeToggle'
import { LogoMark } from '../layout/Logo'

const POINTS: readonly { key: 'parc' | 'achats' | 'finance'; icon: LucideIcon }[] = [
  { key: 'parc', icon: Building2 },
  { key: 'achats', icon: ClipboardCheck },
  { key: 'finance', icon: Wallet },
]

/** Mise en page des ecrans de connexion : panneau de marque + zone de formulaire. */
export function LoginLayout({ children }: { children: ReactNode }) {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation()
  return (
    <div className="grid min-h-dvh bg-background lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
      <aside className="bg-hero relative hidden overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14">
        <div
          className="bg-grid pointer-events-none absolute inset-0 [mask-image:linear-gradient(to_bottom,black,transparent_85%)]"
          aria-hidden
        />
        <div className="relative flex items-center gap-3">
          <LogoMark className="size-10" />
          <div className="leading-tight">
            <p className="font-bold tracking-tight">{tc('app.name')}</p>
            <p className="text-xs text-white/70">{tc('app.fullName')}</p>
          </div>
        </div>
        <div className="relative mt-auto max-w-md">
          <h1 className="text-3xl font-semibold leading-tight tracking-tight xl:text-4xl">
            {t('login.heroTitle')}
          </h1>
          <p className="mt-4 text-base text-white/75">{t('login.heroSubtitle')}</p>
          <ul className="mt-10 grid gap-4">
            {POINTS.map((point) => (
              <li key={point.key} className="flex items-center gap-3 text-sm text-white/90">
                <span className="grid size-9 place-items-center rounded-lg bg-white/10 ring-1 ring-inset ring-white/15">
                  <point.icon className="size-4" aria-hidden />
                </span>
                {t(`login.heroPoints.${point.key}`)}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative mt-16 text-xs text-white/60">{t('login.footer')}</p>
      </aside>
      <main className="relative flex flex-col px-5 py-6 sm:px-10">
        <div className="flex items-center justify-between">
          <span className="flex items-center gap-2.5 lg:invisible">
            <LogoMark />
            <span className="text-sm font-bold">{tc('app.name')}</span>
          </span>
          <ThemeToggle />
        </div>
        <div className="mx-auto flex w-full max-w-xl flex-1 flex-col justify-center py-10">{children}</div>
      </main>
    </div>
  )
}
