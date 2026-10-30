import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import type { Tone } from '../../lib/labels'

const TONES: Record<Tone, string> = {
  brand: 'bg-brand-50 text-brand-700 ring-1 ring-inset ring-brand-200',
  accent: 'bg-accent-50 text-accent-700 ring-1 ring-inset ring-accent-200',
  success: 'bg-success-50 text-success-700 ring-1 ring-inset ring-success-200',
  danger: 'bg-danger-50 text-danger-700 ring-1 ring-inset ring-danger-200',
  warning: 'bg-warning-50 text-warning-700 ring-1 ring-inset ring-warning-100',
  neutral: 'bg-ink-100 text-ink-600 ring-1 ring-inset ring-ink-200',
  info: 'bg-brand-50 text-brand-600 ring-1 ring-inset ring-brand-100',
}

type Props = {
  tone?: Tone
  children: ReactNode
  className?: string
  dot?: boolean
}

export function Badge({ tone = 'neutral', children, className, dot }: Props) {
  return (
    <span className={cn('inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold', TONES[tone], className)}>
      {dot ? <span className="h-1.5 w-1.5 rounded-full bg-current" /> : null}
      {children}
    </span>
  )
}
