import type { ReactNode } from 'react'
import { Icon, type IconName } from './Icon'

export function PageHeader({
  eyebrow,
  title,
  description,
  icon,
  actions,
}: {
  eyebrow?: string
  title: string
  description?: string
  icon?: IconName
  actions?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div className="flex items-start gap-3">
        {icon ? (
          <span className="mt-0.5 grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand-900 text-accent-400 shadow-sm">
            <Icon name={icon} size={22} />
          </span>
        ) : null}
        <div>
          {eyebrow ? <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-500">{eyebrow}</p> : null}
          <h1 className="text-xl font-semibold tracking-tight text-brand-950 sm:text-2xl">{title}</h1>
          {description ? <p className="mt-1 max-w-3xl text-sm text-ink-500">{description}</p> : null}
        </div>
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  )
}
