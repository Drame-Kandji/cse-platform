import { forwardRef, type HTMLAttributes, type ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

/** Conteneur de contenu : plat, bordure fine, coins xl, sans ombre. */
export const Card = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement> & { interactive?: boolean }>(
  function Card({ className, interactive, ...props }, ref) {
    return (
      <div
        ref={ref}
        className={cn(
          'rounded-xl border border-border bg-surface text-foreground',
          interactive && 'transition-colors hover:border-border-strong',
          className,
        )}
        {...props}
      />
    )
  },
)

export interface CardHeaderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  title?: ReactNode
  description?: ReactNode
  /** Actions alignees a droite (boutons, menu). */
  action?: ReactNode
  /** Conserve pour compatibilite : n'est plus affiche (pas de tuile d'icone, charte sobre). */
  icon?: ReactNode
}

/** En-tete de carte : titre, description et actions. */
export function CardHeader({
  className,
  title,
  description,
  action,
  icon: _icon,
  children,
  ...props
}: CardHeaderProps) {
  return (
    <div className={cn('flex items-start gap-3 px-5 pb-0 pt-5', className)} {...props}>
      <div className="min-w-0 flex-1">
        {title && <CardTitle>{title}</CardTitle>}
        {description && <CardDescription>{description}</CardDescription>}
        {children}
      </div>
      {action && <div className="flex shrink-0 items-center gap-2">{action}</div>}
    </div>
  )
}

export function CardTitle({ className, children, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3 className={cn('text-base font-semibold leading-6 tracking-tight', className)} {...props}>
      {children}
    </h3>
  )
}

export function CardDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return <p className={cn('mt-0.5 text-sm text-muted-foreground', className)} {...props} />
}

export function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('p-5', className)} {...props} />
}

export function CardFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'flex items-center justify-end gap-2 rounded-b-xl border-t border-border px-5 py-3',
        className,
      )}
      {...props}
    />
  )
}
