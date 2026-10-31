import { parseISO } from 'date-fns'
import { useEffect, useState } from 'react'
import { cn } from '@/shared/lib/cn'
import { toIsoDate } from '@/shared/lib/format'
import type { DateRange } from '@/shared/ui/calendar'
import { DateRangePicker } from '@/shared/ui/date-picker'
import { PERIOD_PRESETS, type Period, type PeriodPreset } from '../lib/period'

export interface PeriodSelectorProps {
  value: Period
  onChange: (period: { preset: PeriodPreset; du?: string; au?: string }) => void
  className?: string
}

function rangeOf(period: Period): DateRange {
  return period.preset === 'custom'
    ? { from: parseISO(period.du), to: parseISO(period.au) }
    : { from: null, to: null }
}

/** Choix de la periode d'analyse : 30 / 90 jours, 12 mois ou plage personnalisee. */
export function PeriodSelector({ value, onChange, className }: PeriodSelectorProps) {
  const [draft, setDraft] = useState<DateRange>(() => rangeOf(value))
  useEffect(() => {
    setDraft(rangeOf(value))
    // eslint-disable-next-line react-hooks/exhaustive-deps -- resynchronise sur la periode appliquee
  }, [value.preset, value.du, value.au])
  return (
    <div className={cn('flex flex-wrap items-center gap-2', className)}>
      <div
        role="radiogroup"
        aria-label="Période d'analyse"
        className="inline-flex rounded-lg border border-border bg-surface p-0.5 shadow-xs"
      >
        {PERIOD_PRESETS.map((preset) => {
          const active = value.preset === preset.value
          return (
            <button
              key={preset.value}
              type="button"
              role="radio"
              aria-checked={active}
              onClick={() => {
                onChange({ preset: preset.value })
              }}
              className={cn(
                'h-9 rounded-md px-3 text-sm font-medium transition-colors sm:h-8',
                active
                  ? 'bg-primary text-primary-foreground shadow-xs'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground',
              )}
            >
              {preset.label}
            </button>
          )
        })}
      </div>
      <DateRangePicker
        className={cn('w-auto min-w-56', value.preset === 'custom' && '[&_button]:border-primary/40')}
        placeholder="Plage personnalisée"
        max={new Date()}
        value={draft}
        onChange={(range) => {
          setDraft(range)
          if (range.from && range.to) {
            onChange({ preset: 'custom', du: toIsoDate(range.from), au: toIsoDate(range.to) })
          } else if (!range.from && value.preset === 'custom') {
            onChange({ preset: '90' })
          }
        }}
      />
    </div>
  )
}
