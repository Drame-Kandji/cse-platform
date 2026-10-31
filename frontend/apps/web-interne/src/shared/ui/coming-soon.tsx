import { CircleDashed } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from './badge'
import { Card } from './card'
import { PageHeader } from './page-header'
import { Skeleton } from './skeleton'

export interface ComingSoonProps {
  title: string
  description: string
  icon: ReactNode
  /** Fonctionnalites prevues (liste courte, verbes a l'infinitif). */
  features: readonly string[]
  /** Contenu additionnel au-dessus de la carte (ex. message d'accueil). */
  children?: ReactNode
}

/**
 * Emplacement temporaire d'un module non encore livre.
 * A remplacer par la vraie page : ne pas l'utiliser pour du contenu definitif.
 */
export function ComingSoon({ title, description, icon, features, children }: ComingSoonProps) {
  const { t } = useTranslation()
  return (
    <>
      <PageHeader
        title={title}
        description={description}
        meta={
          <Badge tone="accent" dot>
            {t('comingSoon.badge')}
          </Badge>
        }
      />
      {children}
      <Card className="overflow-hidden">
        <div className="grid lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <div className="p-6 sm:p-8">
            <div className="grid size-11 place-items-center rounded-xl bg-primary/10 text-primary-emphasis [&_svg]:size-5">
              {icon}
            </div>
            <h2 className="mt-5 text-lg font-semibold tracking-tight">{t('comingSoon.title')}</h2>
            <p className="mt-1.5 max-w-md text-sm text-muted-foreground">{t('comingSoon.description')}</p>
            <p className="mt-6 text-2xs font-semibold uppercase tracking-[0.08em] text-muted-foreground">
              {t('comingSoon.planned')}
            </p>
            <ul className="mt-3 grid gap-2.5">
              {features.map((feature) => (
                <li key={feature} className="flex items-start gap-2.5 text-sm">
                  <CircleDashed className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
                  {feature}
                </li>
              ))}
            </ul>
          </div>
          <div
            className="relative hidden border-l border-border bg-surface-sunken/70 p-8 lg:block"
            aria-hidden
          >
            <div className="rounded-xl border border-border bg-surface p-4 shadow-card">
              <div className="flex items-center gap-2">
                <Skeleton className="h-8 w-48" />
                <Skeleton className="ml-auto h-8 w-20" />
              </div>
              <div className="mt-4 grid gap-3">
                {[0.9, 0.75, 0.85, 0.6, 0.8].map((width) => (
                  <div key={width} className="flex items-center gap-3">
                    <Skeleton className="size-6 rounded-full" />
                    <Skeleton className="h-3.5" style={{ width: `${width * 60}%` }} />
                    <Skeleton className="ml-auto h-5 w-16 rounded-md" />
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </Card>
    </>
  )
}
