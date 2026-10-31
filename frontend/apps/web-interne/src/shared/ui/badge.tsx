import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'
import type { Tone } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { dotToneClasses, softToneClasses, solidToneClasses } from './tone'

const badgeVariants = cva(
  'inline-flex max-w-full items-center gap-1.5 whitespace-nowrap rounded-full font-medium ring-1 ring-inset [&_svg]:size-3.5',
  {
    variants: {
      size: { sm: 'h-5 px-2 text-2xs', md: 'h-6 px-2.5 text-xs' },
    },
    defaultVariants: { size: 'md' },
  },
)

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {
  tone?: Tone
  variant?: 'soft' | 'solid' | 'outline'
  /** Affiche une pastille de couleur avant le libelle. */
  dot?: boolean
}

/** Etiquette compacte. Pour une valeur d'enum metier, preferer <StatusBadge>. */
export function Badge({
  className,
  tone = 'neutral',
  variant = 'soft',
  size,
  dot,
  children,
  ...props
}: BadgeProps) {
  const toneClass =
    variant === 'solid'
      ? solidToneClasses[tone]
      : variant === 'outline'
        ? 'bg-transparent text-muted-foreground ring-border-strong'
        : softToneClasses[tone]
  return (
    <span className={cn(badgeVariants({ size }), toneClass, className)} {...props}>
      {dot && <span className={cn('size-1.5 shrink-0 rounded-full', dotToneClasses[tone])} aria-hidden />}
      {/* Preflight rend les SVG `block` : on les remet en ligne pour garder icone + libelle sur une ligne. */}
      <span className="truncate [&_svg]:-mt-px [&_svg]:mr-1 [&_svg]:inline-block [&_svg]:align-middle">
        {children}
      </span>
    </span>
  )
}
