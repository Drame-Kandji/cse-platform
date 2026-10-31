import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import { type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { overlayClasses } from './dialog'

export const Sheet = DialogPrimitive.Root
export const SheetTrigger = DialogPrimitive.Trigger
export const SheetClose = DialogPrimitive.Close

const sides = {
  right: 'inset-y-0 right-0 h-full w-full border-l data-[state=open]:animate-slide-in-right',
  left: 'inset-y-0 left-0 h-full w-[85vw] max-w-xs border-r data-[state=open]:animate-slide-in-left',
  bottom:
    'inset-x-0 bottom-0 max-h-[90dvh] w-full rounded-t-2xl border-t pb-safe data-[state=open]:animate-slide-in-bottom',
} as const

const widths = { sm: 'sm:max-w-sm', md: 'sm:max-w-md', lg: 'sm:max-w-xl', xl: 'sm:max-w-3xl' } as const

export interface SheetContentProps {
  title: ReactNode
  description?: ReactNode
  side?: keyof typeof sides
  size?: keyof typeof widths
  /** Pied fixe (actions du panneau). */
  footer?: ReactNode
  /** Masque l'en-tete visuel (le titre reste lu par les lecteurs d'ecran). */
  hideHeader?: boolean
  className?: string
  bodyClassName?: string
  children: ReactNode
}

/**
 * Panneau lateral (vue detail, filtres avances, assistant). Plein ecran sur mobile.
 * @example <Sheet open={open} onOpenChange={setOpen}><SheetContent title="Detail">...</SheetContent></Sheet>
 */
export function SheetContent({
  title,
  description,
  side = 'right',
  size = 'md',
  footer,
  hideHeader,
  className,
  bodyClassName,
  children,
}: SheetContentProps) {
  const { t } = useTranslation()
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className={overlayClasses} />
      <DialogPrimitive.Content
        aria-describedby={undefined}
        className={cn(
          'fixed z-50 flex flex-col border-border bg-surface-raised shadow-overlay focus-visible:outline-none',
          sides[side],
          side === 'right' && widths[size],
          className,
        )}
      >
        {hideHeader ? (
          <>
            <DialogPrimitive.Title className="sr-only">{title}</DialogPrimitive.Title>
            <DialogPrimitive.Close
              className="absolute right-3 top-3 z-10 grid size-9 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              aria-label={t('actions.close')}
            >
              <X className="size-4" aria-hidden />
            </DialogPrimitive.Close>
          </>
        ) : (
          <div className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
            <div className="grid min-w-0 gap-0.5">
              <DialogPrimitive.Title className="truncate text-base font-semibold tracking-tight">
                {title}
              </DialogPrimitive.Title>
              {description && (
                <DialogPrimitive.Description className="text-sm text-muted-foreground">
                  {description}
                </DialogPrimitive.Description>
              )}
            </div>
            <DialogPrimitive.Close
              className="-mr-1.5 grid size-9 shrink-0 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              aria-label={t('actions.close')}
            >
              <X className="size-4" aria-hidden />
            </DialogPrimitive.Close>
          </div>
        )}
        <div className={cn('scrollbar-thin flex-1 overflow-y-auto px-5 py-4', bodyClassName)}>{children}</div>
        {footer && (
          <div className="pb-safe flex flex-col-reverse gap-2 border-t border-border bg-surface-sunken/60 px-5 py-3 sm:flex-row sm:justify-end">
            {footer}
          </div>
        )}
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}
