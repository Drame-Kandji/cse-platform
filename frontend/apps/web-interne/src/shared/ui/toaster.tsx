import { Toaster as Sonner } from 'sonner'
import { useTheme } from '@/shared/theme/theme-context'

/**
 * Conteneur des notifications ephemeres. Utiliser `toast.success(...)` / `toast.error(...)` de "sonner".
 * Les erreurs de mutation sont deja notifiees globalement (voir query-client.ts).
 */
export function Toaster() {
  const { resolvedTheme } = useTheme()
  return (
    <Sonner
      theme={resolvedTheme}
      position="bottom-right"
      closeButton
      duration={5000}
      visibleToasts={4}
      offset={16}
      toastOptions={{
        classNames: {
          toast:
            'group !rounded-xl !border !border-border !bg-surface-raised !text-foreground !shadow-overlay !font-sans',
          title: '!text-sm !font-medium',
          description: '!text-muted-foreground',
          actionButton: '!bg-primary !text-primary-foreground',
          cancelButton: '!bg-muted !text-foreground',
          closeButton: '!border-border !bg-surface-raised !text-muted-foreground',
          success: '[&_[data-icon]]:!text-success-emphasis',
          error: '[&_[data-icon]]:!text-danger-emphasis',
          warning: '[&_[data-icon]]:!text-warning-emphasis',
          info: '[&_[data-icon]]:!text-info-emphasis',
        },
      }}
    />
  )
}
