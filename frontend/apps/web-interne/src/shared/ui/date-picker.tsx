import { isBefore } from 'date-fns'
import { CalendarDays, X } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { formatDate } from '@/shared/lib/format'
import { Calendar, type DateRange } from './calendar'
import { fieldClasses } from './input'
import { Popover, PopoverContent, PopoverTrigger } from './popover'

interface BaseProps {
  placeholder?: string
  disabled?: boolean
  min?: Date
  max?: Date
  id?: string
  className?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

function Trigger({
  label,
  empty,
  onClear,
  ...props
}: BaseProps & { label: string; empty: boolean; onClear: () => void }) {
  const { t } = useTranslation()
  const { id, disabled, className, placeholder: _placeholder, min: _min, max: _max, ...aria } = props
  return (
    <div className={cn('relative w-full', className)}>
      <PopoverTrigger asChild>
        <button
          type="button"
          id={id}
          disabled={disabled}
          className={cn(fieldClasses, 'flex h-10 items-center gap-2 px-3 text-left sm:h-9', !empty && 'pr-9')}
          {...aria}
        >
          <CalendarDays className="size-4 shrink-0 text-muted-foreground" aria-hidden />
          <span className={cn('truncate tabular-nums', empty && 'text-muted-foreground')}>{label}</span>
        </button>
      </PopoverTrigger>
      {!empty && !disabled && (
        <button
          type="button"
          onClick={onClear}
          aria-label={t('actions.clear')}
          className="absolute right-1.5 top-1/2 grid size-6 -translate-y-1/2 place-items-center rounded text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          <X className="size-3.5" aria-hidden />
        </button>
      )}
    </div>
  )
}

export interface DatePickerProps extends BaseProps {
  value: Date | null
  onChange: (value: Date | null) => void
}

/** Selecteur de date unique (format "12 mars 2026"). Serialiser avec `toIsoDate`. */
export function DatePicker({ value, onChange, placeholder, min, max, ...props }: DatePickerProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <Trigger
        {...props}
        label={value ? formatDate(value) : (placeholder ?? t('date.placeholder'))}
        empty={!value}
        onClear={() => {
          onChange(null)
        }}
      />
      <PopoverContent>
        <Calendar
          selected={value}
          min={min}
          max={max}
          onSelect={(date) => {
            onChange(date)
            setOpen(false)
          }}
        />
      </PopoverContent>
    </Popover>
  )
}

export interface DateRangePickerProps extends BaseProps {
  value: DateRange
  onChange: (value: DateRange) => void
}

/** Selecteur de periode : premier clic = debut, second clic = fin. */
export function DateRangePicker({ value, onChange, placeholder, min, max, ...props }: DateRangePickerProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const label =
    value.from && value.to
      ? `${formatDate(value.from)} au ${formatDate(value.to)}`
      : value.from
        ? `${formatDate(value.from)} au …`
        : (placeholder ?? t('date.rangePlaceholder'))
  const select = (date: Date) => {
    if (!value.from || value.to) {
      onChange({ from: date, to: null })
      return
    }
    const [from, to] = isBefore(date, value.from) ? [date, value.from] : [value.from, date]
    onChange({ from, to })
    setOpen(false)
  }
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <Trigger
        {...props}
        label={label}
        empty={!value.from}
        onClear={() => {
          onChange({ from: null, to: null })
        }}
      />
      <PopoverContent>
        <Calendar range={value} min={min} max={max} onSelect={select} />
      </PopoverContent>
    </Popover>
  )
}
