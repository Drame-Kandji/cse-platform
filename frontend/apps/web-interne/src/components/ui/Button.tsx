import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Icon, type IconName } from './Icon'

type Variant = 'primary' | 'accent' | 'success' | 'danger' | 'outline' | 'ghost'
type Size = 'sm' | 'md'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  size?: Size
  icon?: IconName
  iconRight?: IconName
  loading?: boolean
  children?: ReactNode
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-brand-900 text-white hover:bg-brand-800 focus:ring-brand-300',
  accent: 'bg-accent-500 text-brand-950 hover:bg-accent-400 focus:ring-accent-200',
  success: 'bg-success-600 text-white hover:bg-success-700 focus:ring-success-200',
  danger: 'bg-danger-600 text-white hover:bg-danger-700 focus:ring-danger-200',
  outline: 'border border-ink-200 bg-white text-ink-700 hover:border-brand-300 hover:bg-brand-50 focus:ring-brand-200',
  ghost: 'bg-transparent text-ink-600 hover:bg-ink-100 focus:ring-ink-200',
}

export const Button = forwardRef<HTMLButtonElement, Props>(function Button(
  { variant = 'primary', size = 'md', icon, iconRight, loading, className, children, disabled, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'h-8 px-3 text-xs' : 'h-10 px-4 text-sm',
        VARIANTS[variant],
        className,
      )}
      {...props}
    >
      {loading ? (
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden />
      ) : icon ? (
        <Icon name={icon} size={size === 'sm' ? 14 : 16} />
      ) : null}
      {children}
      {iconRight && !loading ? <Icon name={iconRight} size={size === 'sm' ? 14 : 16} /> : null}
    </button>
  )
})
