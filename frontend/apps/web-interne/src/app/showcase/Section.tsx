import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

/** Section de la vitrine (ancre + titre). */
export function Section({
  id,
  title,
  description,
  children,
}: {
  id: string
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <section id={id} className="scroll-mt-24 border-t border-border pt-10 first:border-0 first:pt-0">
      <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
      {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
      <div className="mt-6 grid grid-cols-[minmax(0,1fr)] gap-6">{children}</div>
    </section>
  )
}

/** Bloc de demonstration nomme. */
export function Demo({
  title,
  children,
  className,
}: {
  title: string
  children: ReactNode
  className?: string
}) {
  return (
    <div className="flex flex-col gap-3">
      <p className="text-2xs font-semibold uppercase tracking-[0.08em] text-muted-foreground">{title}</p>
      <div
        className={cn(
          'flex-1 rounded-xl border border-dashed border-border-strong/70 bg-surface p-5',
          className,
        )}
      >
        {children}
      </div>
    </div>
  )
}
