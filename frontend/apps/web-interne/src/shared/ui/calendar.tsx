import {
  addDays,
  addMonths,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  isAfter,
  isBefore,
  isSameDay,
  isSameMonth,
  isToday,
  isWithinInterval,
  startOfMonth,
  startOfWeek,
} from 'date-fns'
import { fr } from 'date-fns/locale'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useState, type KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'

export interface DateRange {
  readonly from: Date | null
  readonly to: Date | null
}

export interface CalendarProps {
  /** Date unique selectionnee. */
  selected?: Date | null
  /** Plage selectionnee (mode periode). */
  range?: DateRange
  onSelect: (date: Date) => void
  min?: Date
  max?: Date
  /** Mois affiche initialement. */
  defaultMonth?: Date
}

const WEEK_OPTIONS = { weekStartsOn: 1 } as const
const KEY_OFFSETS: Readonly<Record<string, number>> = {
  ArrowLeft: -1,
  ArrowRight: 1,
  ArrowUp: -7,
  ArrowDown: 7,
}

/** Calendrier mensuel francais (semaine du lundi), navigable au clavier (fleches, PageUp/PageDown). */
export function Calendar({ selected, range, onSelect, min, max, defaultMonth }: CalendarProps) {
  const { t } = useTranslation()
  const initial = defaultMonth ?? selected ?? range?.from ?? new Date()
  const [month, setMonth] = useState(() => startOfMonth(initial))
  const [focused, setFocused] = useState<Date>(initial)
  const days = eachDayOfInterval({
    start: startOfWeek(startOfMonth(month), WEEK_OPTIONS),
    end: endOfWeek(endOfMonth(month), WEEK_OPTIONS),
  })
  const weekdays = days.slice(0, 7).map((day) => format(day, 'EEEEEE', { locale: fr }))
  const isDisabled = (day: Date) =>
    Boolean((min && isBefore(day, min) && !isSameDay(day, min)) || (max && isAfter(day, max)))

  const moveFocus = (next: Date) => {
    setFocused(next)
    if (!isSameMonth(next, month)) setMonth(startOfMonth(next))
    requestAnimationFrame(() => {
      document.querySelector<HTMLButtonElement>(`[data-day="${format(next, 'yyyy-MM-dd')}"]`)?.focus()
    })
  }
  const onKeyDown = (event: KeyboardEvent) => {
    const offset = KEY_OFFSETS[event.key]
    if (offset !== undefined) moveFocus(addDays(focused, offset))
    else if (event.key === 'PageUp') moveFocus(addMonths(focused, -1))
    else if (event.key === 'PageDown') moveFocus(addMonths(focused, 1))
    else return
    event.preventDefault()
  }

  return (
    <div className="w-[17.5rem] p-3">
      <div className="mb-2 flex items-center justify-between">
        <button
          type="button"
          onClick={() => {
            setMonth(addMonths(month, -1))
          }}
          aria-label={t('date.previousMonth')}
          className="grid size-8 place-items-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          <ChevronLeft className="size-4" aria-hidden />
        </button>
        <p className="text-sm font-semibold capitalize" aria-live="polite">
          {format(month, 'LLLL yyyy', { locale: fr })}
        </p>
        <button
          type="button"
          onClick={() => {
            setMonth(addMonths(month, 1))
          }}
          aria-label={t('date.nextMonth')}
          className="grid size-8 place-items-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          <ChevronRight className="size-4" aria-hidden />
        </button>
      </div>
      <div
        role="grid"
        tabIndex={-1}
        className="grid grid-cols-7 gap-y-0.5 focus-visible:ring-0"
        onKeyDown={onKeyDown}
      >
        {weekdays.map((day) => (
          <span
            key={day}
            role="columnheader"
            className="grid h-8 place-items-center text-2xs font-medium uppercase text-muted-foreground"
          >
            {day}
          </span>
        ))}
        {days.map((day) => {
          const isSelected =
            (selected && isSameDay(day, selected)) ||
            (range?.from && isSameDay(day, range.from)) ||
            (range?.to && isSameDay(day, range.to))
          const inRange =
            range?.from && range.to && isWithinInterval(day, { start: range.from, end: range.to })
          const outside = !isSameMonth(day, month)
          return (
            <button
              key={day.toISOString()}
              type="button"
              role="gridcell"
              data-day={format(day, 'yyyy-MM-dd')}
              tabIndex={isSameDay(day, focused) ? 0 : -1}
              disabled={isDisabled(day)}
              aria-selected={Boolean(isSelected)}
              aria-label={format(day, 'EEEE d MMMM yyyy', { locale: fr })}
              onClick={() => {
                setFocused(day)
                onSelect(day)
              }}
              className={cn(
                'grid h-9 place-items-center rounded-md text-sm tabular-nums transition-colors hover:bg-muted focus-visible:ring-offset-0 disabled:pointer-events-none disabled:opacity-30',
                outside && 'text-muted-foreground/60',
                inRange && !isSelected && 'rounded-none bg-primary/10 text-primary-emphasis',
                isToday(day) && !isSelected && 'font-semibold text-primary-emphasis',
                isSelected && 'bg-primary font-medium text-primary-foreground hover:bg-primary/90',
              )}
            >
              {format(day, 'd')}
            </button>
          )
        })}
      </div>
    </div>
  )
}
