import type { HTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

/** Touche clavier (raccourcis). */
export function Kbd({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return (
    <kbd
      className={cn(
        'inline-flex h-5 min-w-5 items-center justify-center rounded border border-border-strong/70 bg-surface-sunken px-1 font-sans text-2xs font-medium text-muted-foreground',
        className,
      )}
      {...props}
    />
  )
}
