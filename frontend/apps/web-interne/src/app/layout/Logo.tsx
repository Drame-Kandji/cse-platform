import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'

/** Monogramme CSE (batiment stylise + bande de securite ambre). */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={cn('size-8 shrink-0', className)} aria-hidden>
      <rect width="32" height="32" rx="8" className="fill-primary" />
      <path
        d="M8 23V12.5L16 8l8 4.5V23"
        fill="none"
        stroke="white"
        strokeWidth="2.4"
        strokeLinejoin="round"
      />
      <rect x="12.5" y="16" width="7" height="7" rx="1.2" className="fill-accent" />
    </svg>
  )
}

/** Logo complet : monogramme + nom (masque en mode replie). */
export function Logo({ collapsed = false, className }: { collapsed?: boolean; className?: string }) {
  const { t } = useTranslation()
  return (
    <span className={cn('flex min-w-0 items-center gap-2.5', className)}>
      <LogoMark />
      {!collapsed && (
        <span className="grid min-w-0 leading-tight">
          <span className="text-sm font-bold tracking-tight text-foreground">{t('app.name')}</span>
          <span className="truncate text-2xs font-medium text-muted-foreground">{t('app.product')}</span>
        </span>
      )}
    </span>
  )
}
