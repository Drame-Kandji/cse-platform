import * as AlertPrimitive from '@radix-ui/react-alert-dialog'
import { AlertTriangle } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { Button } from './button'
import { overlayClasses } from './dialog'

export interface ConfirmDialogProps {
  title: ReactNode
  description: ReactNode
  confirmLabel?: string
  cancelLabel?: string
  /** "danger" pour une action destructive ou irreversible. */
  tone?: 'default' | 'danger'
  /** Peut etre asynchrone : le bouton affiche un chargement et le dialogue reste ouvert jusqu'a la fin. */
  onConfirm: () => void | Promise<void>
  /** Element declencheur (sinon piloter avec open/onOpenChange). */
  trigger?: ReactNode
  open?: boolean
  onOpenChange?: (open: boolean) => void
  /** Contenu additionnel (ex. champ motif de rejet). */
  children?: ReactNode
}

/**
 * Confirmation d'une action sensible (suppression, rejet, blacklistage).
 * @example <ConfirmDialog tone="danger" title="Rejeter le besoin ?" description="..." onConfirm={rejeter} trigger={<Button>Rejeter</Button>} />
 */
export function ConfirmDialog({
  title,
  description,
  confirmLabel,
  cancelLabel,
  tone = 'default',
  onConfirm,
  trigger,
  open,
  onOpenChange,
  children,
}: ConfirmDialogProps) {
  const { t } = useTranslation()
  const [internalOpen, setInternalOpen] = useState(false)
  const [pending, setPending] = useState(false)
  const isOpen = open ?? internalOpen
  const setOpen = (next: boolean) => {
    if (pending) return
    setInternalOpen(next)
    onOpenChange?.(next)
  }
  const confirm = async () => {
    setPending(true)
    try {
      await onConfirm()
      setInternalOpen(false)
      onOpenChange?.(false)
    } finally {
      setPending(false)
    }
  }
  return (
    <AlertPrimitive.Root open={isOpen} onOpenChange={setOpen}>
      {trigger && <AlertPrimitive.Trigger asChild>{trigger}</AlertPrimitive.Trigger>}
      <AlertPrimitive.Portal>
        <AlertPrimitive.Overlay className={overlayClasses} />
        <AlertPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100vw-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-border bg-surface-raised p-6 shadow-overlay data-[state=open]:animate-zoom-in">
          <div className="flex gap-4">
            {tone === 'danger' && (
              <div className="grid size-10 shrink-0 place-items-center rounded-full bg-danger/10 text-danger-emphasis">
                <AlertTriangle className="size-5" aria-hidden />
              </div>
            )}
            <div className="grid flex-1 gap-1.5">
              <AlertPrimitive.Title className="text-base font-semibold tracking-tight">
                {title}
              </AlertPrimitive.Title>
              <AlertPrimitive.Description className="text-sm text-muted-foreground">
                {description}
              </AlertPrimitive.Description>
              {children && <div className="mt-2">{children}</div>}
            </div>
          </div>
          <div className={cn('mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end')}>
            <AlertPrimitive.Cancel asChild>
              <Button variant="secondary" disabled={pending}>
                {cancelLabel ?? t('actions.cancel')}
              </Button>
            </AlertPrimitive.Cancel>
            <Button
              variant={tone === 'danger' ? 'danger' : 'primary'}
              loading={pending}
              onClick={() => {
                void confirm()
              }}
            >
              {confirmLabel ?? t('actions.confirm')}
            </Button>
          </div>
        </AlertPrimitive.Content>
      </AlertPrimitive.Portal>
    </AlertPrimitive.Root>
  )
}
