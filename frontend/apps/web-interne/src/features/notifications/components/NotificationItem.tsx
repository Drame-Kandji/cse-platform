import { describeStatus } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatDateTime, formatRelative } from '@/shared/lib/format'
import type { Notification } from '../types'

export interface NotificationItemProps {
  notification: Notification
  onOpen: (notification: Notification) => void
  /** Affichage dense (popover) : message tronque a deux lignes. */
  compact?: boolean
}

/**
 * Ligne de notification cliquable : titre, message, date relative, pastille bleue si non lue.
 * Le niveau n'est affiche (en rouge) que s'il est critique ; sinon il reste accessible au lecteur d'ecran.
 */
export function NotificationItem({ notification, onOpen, compact }: NotificationItemProps) {
  const niveau = describeStatus('niveauNotification', notification.niveau)
  const critique = notification.niveau === 'CRITIQUE'
  return (
    <button
      type="button"
      onClick={() => {
        onOpen(notification)
      }}
      className="flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-muted focus-visible:bg-muted focus-visible:outline-none"
    >
      <span
        className={cn(
          'mt-1.5 size-2 shrink-0 rounded-full',
          notification.lue ? 'bg-transparent' : 'bg-primary',
        )}
      >
        {!notification.lue && <span className="sr-only">Non lue</span>}
      </span>
      <span className="grid min-w-0 flex-1 gap-0.5">
        <span className={cn('text-sm', notification.lue ? 'font-medium' : 'font-semibold')}>
          {critique ? (
            <span className="mr-1.5 text-danger-emphasis">{niveau.label} ·</span>
          ) : (
            <span className="sr-only">{niveau.label} : </span>
          )}
          {notification.titre}
        </span>
        <span className={cn('text-sm text-muted-foreground', compact && 'line-clamp-2')}>
          {notification.message}
        </span>
        <time
          dateTime={notification.createdAt}
          title={formatDateTime(notification.createdAt)}
          className="text-xs text-muted-foreground"
        >
          {formatRelative(notification.createdAt)}
        </time>
      </span>
    </button>
  )
}
