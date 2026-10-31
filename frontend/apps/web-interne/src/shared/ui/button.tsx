import { Slot } from '@radix-ui/react-slot'
import { type VariantProps } from 'class-variance-authority'
import { Loader2 } from 'lucide-react'
import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'
import { buttonVariants } from './button-variants'
import { Tooltip } from './tooltip'

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
  /** Affiche un indicateur de chargement et desactive le bouton. */
  loading?: boolean
  /** Icone placee avant le libelle. */
  leftIcon?: ReactNode
  /** Icone placee apres le libelle. */
  rightIcon?: ReactNode
  /** Rend l'enfant (ex. <Link>) avec le style du bouton. */
  asChild?: boolean
}

/** Bouton d'action principal du design system. */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    className,
    variant,
    size,
    loading = false,
    leftIcon,
    rightIcon,
    asChild = false,
    children,
    disabled,
    type,
    ...props
  },
  ref,
) {
  const classes = cn(buttonVariants({ variant, size }), className)
  if (asChild) {
    return (
      <Slot ref={ref} className={classes} {...props}>
        {children}
      </Slot>
    )
  }
  return (
    <button
      ref={ref}
      type={type ?? 'button'}
      className={classes}
      disabled={disabled ?? loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading ? <Loader2 className="animate-spin" aria-hidden /> : leftIcon}
      {children}
      {!loading && rightIcon}
    </button>
  )
})

export interface IconButtonProps extends Omit<ButtonProps, 'leftIcon' | 'rightIcon' | 'children' | 'size'> {
  /** Libelle accessible (aria-label) et contenu de l'infobulle. */
  label: string
  icon: ReactNode
  size?: 'icon' | 'icon-sm'
  /** Affiche le libelle en infobulle au survol (vrai par defaut). */
  tooltip?: boolean
}

/** Bouton icone seul : libelle accessible obligatoire. */
export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { label, icon, variant = 'ghost', size = 'icon', tooltip = true, ...props },
  ref,
) {
  const button = (
    <Button ref={ref} variant={variant} size={size} aria-label={label} {...props}>
      {icon}
    </Button>
  )
  return tooltip ? <Tooltip content={label}>{button}</Tooltip> : button
})
