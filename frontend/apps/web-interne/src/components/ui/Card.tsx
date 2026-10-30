import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Icon, type IconName } from './Icon'
import { Badge } from './Badge'
import type { Tone } from '../../lib/labels'

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('rounded-panel border border-ink-200 bg-white shadow-card', className)}>{children}</div>
}

export function CardHeader({
  title,
  subtitle,
  action,
  icon,
}: {
  title: string
  subtitle?: string
  action?: ReactNode
  icon?: IconName
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-ink-100 px-5 py-4">
      <div className="flex items-start gap-3">
        {icon ? (
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-brand-50 text-brand-700">
            <Icon name={icon} size={18} />
          </span>
        ) : null}
        <div>
          <h3 className="text-sm font-semibold text-brand-950">{title}</h3>
          {subtitle ? <p className="mt-0.5 text-xs text-ink-500">{subtitle}</p> : null}
        </div>
      </div>
      {action}
    </div>
  )
}

const STAT_TONES: Record<Tone, { icon: string; value: string }> = {
  brand: { icon: 'bg-brand-50 text-brand-700', value: 'text-brand-950' },
  accent: { icon: 'bg-accent-50 text-accent-700', value: 'text-brand-950' },
  success: { icon: 'bg-success-50 text-success-700', value: 'text-success-700' },
  danger: { icon: 'bg-danger-50 text-danger-700', value: 'text-danger-700' },
  warning: { icon: 'bg-warning-50 text-warning-700', value: 'text-warning-700' },
  neutral: { icon: 'bg-ink-100 text-ink-600', value: 'text-brand-950' },
  info: { icon: 'bg-brand-50 text-brand-600', value: 'text-brand-950' },
}

export function StatCard({
  label,
  value,
  hint,
  icon,
  tone = 'brand',
  badge,
}: {
  label: string
  value: ReactNode
  hint?: ReactNode
  icon: IconName
  tone?: Tone
  badge?: ReactNode
}) {
  const tones = STAT_TONES[tone]
  return (
    <div className="rounded-panel border border-ink-200 bg-white p-4 shadow-card">
      <div className="flex items-start justify-between gap-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">{label}</p>
        <span className={cn('grid h-9 w-9 place-items-center rounded-lg', tones.icon)}>
          <Icon name={icon} size={18} />
        </span>
      </div>
      <p className={cn('mt-3 text-2xl font-semibold tracking-tight', tones.value)}>{value}</p>
      <div className="mt-2 flex items-center justify-between gap-2">
        {hint ? <p className="text-xs text-ink-500">{hint}</p> : <span />}
        {badge}
      </div>
    </div>
  )
}

export function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-ink-100 py-2.5 last:border-0">
      <span className="text-xs text-ink-500">{label}</span>
      <span className="text-right text-sm font-medium text-ink-800">{value}</span>
    </div>
  )
}

export function ScoreBar({ score, showLabel = true }: { score: number; showLabel?: boolean }) {
  const tone = score < 50 ? 'bg-danger-600' : score < 70 ? 'bg-accent-500' : score < 85 ? 'bg-brand-500' : 'bg-success-600'
  return (
    <div className="w-full">
      <div className="h-2 w-full overflow-hidden rounded-full bg-ink-100">
        <div className={cn('h-full rounded-full transition-all', tone)} style={{ width: `${Math.max(0, Math.min(100, score))}%` }} />
      </div>
      {showLabel ? (
        <div className="mt-1 flex items-center justify-between text-[11px] text-ink-500">
          <span>Score performance</span>
          <span className="font-semibold text-ink-700">{score.toFixed(1).replace('.', ',')} / 100</span>
        </div>
      ) : null}
    </div>
  )
}

export function MiniBar({ label, value, max, tone = 'brand' }: { label: string; value: number; max: number; tone?: Tone }) {
  const percent = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0
  const colors: Record<Tone, string> = {
    brand: 'bg-brand-700',
    accent: 'bg-accent-500',
    success: 'bg-success-600',
    danger: 'bg-danger-600',
    warning: 'bg-warning-500',
    neutral: 'bg-ink-400',
    info: 'bg-brand-500',
  }
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-xs">
        <span className="text-ink-500">{label}</span>
        <Badge tone={tone}>{percent} %</Badge>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-ink-100">
        <div className={cn('h-full rounded-full', colors[tone])} style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
