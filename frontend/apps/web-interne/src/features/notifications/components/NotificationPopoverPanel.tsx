import { BellOff, CheckCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { paths } from '@/app/routes'
import { Button } from '@/shared/ui/button'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { useNotifications, useToutLire } from '../hooks/useNotifications'
import { useOpenNotification } from '../hooks/useOpenNotification'
import { NotificationItem } from './NotificationItem'

/** Parametres de la liste du popover (10 dernieres) : partages avec le cache SSE. */
export const LATEST_NOTIFICATIONS = { page: 0, size: 10 } as const

export interface NotificationPopoverPanelProps {
  unreadCount: number
  /** Ferme le popover (navigation vers un lien ou la page complete). */
  onClose: () => void
}

/** Contenu du popover de la cloche : 10 dernieres notifications, "Tout marquer comme lu", lien page. */
export function NotificationPopoverPanel({ unreadCount, onClose }: NotificationPopoverPanelProps) {
  const query = useNotifications(LATEST_NOTIFICATIONS)
  const toutLire = useToutLire()
  const open = useOpenNotification(onClose)
  const items = query.data?.content ?? []

  return (
    <div className="flex max-h-[min(32rem,calc(100dvh-6rem))] flex-col">
      <div className="flex items-center justify-between gap-2 border-b border-border px-4 py-2.5">
        <p className="grid min-w-0 text-sm font-semibold sm:block">
          Notifications
          {unreadCount > 0 && (
            <span className="whitespace-nowrap text-xs font-normal text-muted-foreground sm:ml-2">
              {unreadCount} non lue(s)
            </span>
          )}
        </p>
        <Button
          variant="ghost"
          size="sm"
          className="shrink-0"
          leftIcon={<CheckCheck />}
          disabled={unreadCount === 0}
          loading={toutLire.isPending}
          onClick={() => {
            toutLire.mutate()
          }}
        >
          Tout marquer comme lu
        </Button>
      </div>
      <div className="scrollbar-thin min-h-0 flex-1 overflow-y-auto">
        {query.isPending ? (
          <ul className="grid gap-3 p-4" aria-label="Chargement des notifications">
            {[0, 1, 2].map((index) => (
              <li key={index} className="flex gap-3">
                <Skeleton className="size-8 rounded-lg" />
                <div className="grid flex-1 gap-1.5">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-full" />
                </div>
              </li>
            ))}
          </ul>
        ) : query.isError ? (
          <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
        ) : items.length === 0 ? (
          <EmptyState size="compact" icon={<BellOff aria-hidden />} title="Aucune notification" />
        ) : (
          <ul className="divide-y divide-border">
            {items.map((notification) => (
              <li key={notification.id}>
                <NotificationItem notification={notification} onOpen={open} compact />
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className="border-t border-border p-1.5">
        <Button asChild variant="ghost" size="sm" className="w-full">
          <Link to={paths.notifications} onClick={onClose}>
            Voir toutes les notifications
          </Link>
        </Button>
      </div>
    </div>
  )
}
