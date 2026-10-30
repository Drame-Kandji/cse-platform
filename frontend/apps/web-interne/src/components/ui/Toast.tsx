import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Icon } from './Icon'

type ToastKind = 'success' | 'error' | 'info' | 'warning'

type ToastItem = {
  id: number
  kind: ToastKind
  title: string
  message?: string
}

type ToastContextValue = {
  notify: (toast: Omit<ToastItem, 'id'>) => void
  success: (title: string, message?: string) => void
  error: (title: string, message?: string) => void
  info: (title: string, message?: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

const TONE: Record<ToastKind, { wrap: string; icon: 'checkCircle' | 'alert' | 'info'; label: string }> = {
  success: { wrap: 'border-success-200 bg-success-50 text-success-700', icon: 'checkCircle', label: 'Succès' },
  error: { wrap: 'border-danger-200 bg-danger-50 text-danger-700', icon: 'alert', label: 'Erreur' },
  info: { wrap: 'border-brand-200 bg-brand-50 text-brand-700', icon: 'info', label: 'Information' },
  warning: { wrap: 'border-accent-200 bg-accent-50 text-accent-700', icon: 'alert', label: 'Attention' },
}

let nextId = 1

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const remove = useCallback((id: number) => {
    setItems((current) => current.filter((item) => item.id !== id))
  }, [])

  const notify = useCallback(
    (toast: Omit<ToastItem, 'id'>) => {
      const id = nextId++
      setItems((current) => [...current.slice(-4), { ...toast, id }])
      window.setTimeout(() => remove(id), toast.kind === 'error' ? 7000 : 4500)
    },
    [remove],
  )

  const value = useMemo<ToastContextValue>(
    () => ({
      notify,
      success: (title, message) => notify({ kind: 'success', title, message }),
      error: (title, message) => notify({ kind: 'error', title, message }),
      info: (title, message) => notify({ kind: 'info', title, message }),
    }),
    [notify],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[1200] flex w-full max-w-sm flex-col gap-2">
        {items.map((item) => {
          const tone = TONE[item.kind]
          return (
            <div
              key={item.id}
              role="status"
              className={cn(
                'pointer-events-auto animate-slide-up rounded-panel border p-4 shadow-float',
                tone.wrap,
              )}
            >
              <div className="flex items-start gap-3">
                <Icon name={tone.icon} size={19} className="mt-0.5 shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold">{item.title}</p>
                  {item.message ? <p className="mt-1 text-xs leading-relaxed opacity-90">{item.message}</p> : null}
                </div>
                <button
                  type="button"
                  onClick={() => remove(item.id)}
                  className="rounded p-1 opacity-70 transition hover:bg-white/40 hover:opacity-100"
                  aria-label="Fermer la notification"
                >
                  <Icon name="x" size={14} />
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast doit être utilisé dans ToastProvider')
  return context
}
