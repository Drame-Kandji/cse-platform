import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationsApi } from '../../api/endpoints'
import { getApiErrorMessage } from '../../api/client'
import { useAuth } from '../../app/auth'
import { formatRelative } from '../../lib/format'
import { cn } from '../../lib/cn'
import { Icon } from '../ui/Icon'
import { useToast } from '../ui/Toast'

export function NotificationBell() {
  const { user } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement | null>(null)

  const query = useQuery({
    queryKey: ['notifications', user?.email],
    queryFn: () => notificationsApi.list(user?.email),
    enabled: Boolean(user?.email),
    refetchInterval: 45_000,
  })

  const notifications = useMemo(() => query.data ?? [], [query.data])
  const unread = notifications.filter((item) => !item.lue).length

  const markRead = useMutation({
    mutationFn: (id: string) => notificationsApi.marquerLue(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (error) => toast.error('Notification non mise à jour', getApiErrorMessage(error)),
  })

  useEffect(() => {
    if (!open) return
    const onClick = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [open])

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="relative grid h-10 w-10 place-items-center rounded-lg border border-ink-200 bg-white text-ink-600 transition hover:border-brand-300 hover:text-brand-700"
        aria-label={`Notifications${unread ? ` (${unread} non lues)` : ''}`}
        aria-expanded={open}
      >
        <Icon name="bell" size={19} />
        {unread > 0 ? (
          <span className="absolute -right-1 -top-1 grid h-5 min-w-5 place-items-center rounded-full bg-danger-600 px-1 text-[10px] font-bold text-white ring-2 ring-white">
            {unread > 9 ? '9+' : unread}
          </span>
        ) : null}
      </button>

      {open ? (
        <div className="absolute right-0 top-12 z-[900] w-[360px] max-w-[calc(100vw-2rem)] animate-slide-up overflow-hidden rounded-panel border border-ink-200 bg-white shadow-float">
          <div className="flex items-center justify-between border-b border-ink-100 px-4 py-3">
            <div>
              <p className="text-sm font-semibold text-brand-950">Notifications</p>
              <p className="text-[11px] text-ink-500">{unread} non lue{unread > 1 ? 's' : ''} · {notifications.length} au total</p>
            </div>
            <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-50 text-brand-700">
              <Icon name="bell" size={16} />
            </span>
          </div>
          <div className="max-h-[360px] overflow-y-auto">
            {query.isLoading ? <p className="px-4 py-6 text-center text-xs text-ink-500">Chargement…</p> : null}
            {query.isError ? (
              <p className="px-4 py-6 text-center text-xs text-danger-600">{getApiErrorMessage(query.error)}</p>
            ) : null}
            {!query.isLoading && notifications.length === 0 ? (
              <div className="px-4 py-8 text-center">
                <Icon name="bell" className="mx-auto text-ink-300" size={24} />
                <p className="mt-2 text-xs text-ink-500">Aucune notification pour {user?.email}.</p>
              </div>
            ) : null}
            {notifications.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  if (!item.lue) markRead.mutate(item.id)
                }}
                className={cn(
                  'flex w-full gap-3 border-b border-ink-100 px-4 py-3 text-left transition last:border-0 hover:bg-brand-50/60',
                  !item.lue && 'bg-brand-50/40',
                )}
              >
                <span
                  className={cn(
                    'mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-full',
                    item.lue ? 'bg-ink-100 text-ink-400' : 'bg-accent-50 text-accent-700',
                  )}
                >
                  <Icon name={item.type.includes('ENGAGEMENT') ? 'wallet' : item.type.includes('BESOIN') ? 'clipboard' : 'info'} size={15} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-start justify-between gap-2">
                    <span className={cn('text-xs leading-snug', item.lue ? 'font-medium text-ink-600' : 'font-semibold text-brand-950')}>
                      {item.sujet}
                    </span>
                    {!item.lue ? <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-danger-500" /> : null}
                  </span>
                  <span className="mt-1 block text-[11px] leading-relaxed text-ink-500">{item.message}</span>
                  <span className="mt-1 block text-[10px] text-ink-400">{formatRelative(item.createdAt)}</span>
                </span>
              </button>
            ))}
          </div>
          <div className="border-t border-ink-100 bg-ink-100/40 px-4 py-2 text-center text-[10px] text-ink-500">
            Destinataire : {user?.email}
          </div>
        </div>
      ) : null}
    </div>
  )
}
