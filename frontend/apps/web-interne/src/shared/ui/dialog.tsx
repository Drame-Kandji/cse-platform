import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import {
  forwardRef,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'

export const Dialog = DialogPrimitive.Root
export const DialogTrigger = DialogPrimitive.Trigger
export const DialogClose = DialogPrimitive.Close

/** Voile commun aux dialogues et panneaux lateraux. */
export const overlayClasses =
  'fixed inset-0 z-50 bg-[rgb(2_6_23/0.45)] backdrop-blur-[2px] data-[state=open]:animate-fade-in data-[state=closed]:animate-fade-out'

const sizes = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' } as const

export interface DialogContentProps extends Omit<
  ComponentPropsWithoutRef<typeof DialogPrimitive.Content>,
  'title'
> {
  /** Titre obligatoire (lu par les lecteurs d'ecran). */
  title: ReactNode
  description?: ReactNode
  size?: keyof typeof sizes
  hideClose?: boolean
}

/** Fenetre modale centree. Le pied de page se compose avec <DialogFooter>. */
export const DialogContent = forwardRef<ElementRef<typeof DialogPrimitive.Content>, DialogContentProps>(
  function DialogContent({ className, title, description, size = 'md', hideClose, children, ...props }, ref) {
    const { t } = useTranslation()
    return (
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className={overlayClasses} />
        <DialogPrimitive.Content
          ref={ref}
          aria-describedby={description ? undefined : props['aria-describedby']}
          className={cn(
            'fixed left-1/2 top-1/2 z-50 grid max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] -translate-x-1/2 -translate-y-1/2 gap-0 overflow-hidden rounded-2xl border border-border bg-surface-raised shadow-overlay data-[state=open]:animate-zoom-in',
            sizes[size],
            className,
          )}
          {...props}
        >
          <div className="flex items-start justify-between gap-4 px-6 pb-2 pt-5">
            <div className="grid gap-1">
              <DialogPrimitive.Title className="text-base font-semibold tracking-tight">
                {title}
              </DialogPrimitive.Title>
              {description && (
                <DialogPrimitive.Description className="text-sm text-muted-foreground">
                  {description}
                </DialogPrimitive.Description>
              )}
            </div>
            {!hideClose && (
              <DialogPrimitive.Close
                className="-mr-2 -mt-1 grid size-8 shrink-0 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                aria-label={t('actions.close')}
              >
                <X className="size-4" aria-hidden />
              </DialogPrimitive.Close>
            )}
          </div>
          <div className="overflow-y-auto px-6 py-3">{children}</div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    )
  },
)

/** Pied de dialogue (actions alignees a droite, empilees sur mobile). */
export function DialogFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        '-mx-6 -mb-3 mt-3 flex flex-col-reverse gap-2 border-t border-border bg-surface-sunken/60 px-6 py-3 sm:flex-row sm:justify-end',
        className,
      )}
      {...props}
    />
  )
}
