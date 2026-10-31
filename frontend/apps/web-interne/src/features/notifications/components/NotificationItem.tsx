import { CircleCheck, Info, OctagonAlert, TriangleAlert, type LucideIcon } from 'lucide-react'
import { describeStatus } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatDateTime, formatRelative } from '@/shared/lib/format'
import { softToneClasses } from '@/shared/ui/tone'
import type { Notification } from '../types'

const ICONS: Readonly<Partial<Record<string, LucideIcon>>> = {
  INFO: Info,
  SUCCES: CircleCheck,
  ALERTE: TriangleAlert,
  CRITIQUE: OctagonAlert,
}

export interface NotificationItemProps {
  notification: Notification
  onOpen: (notification: Notification) => void
  /** Affichage dense (popover) : message tronque a deux lignes. */
  compact?: boolean
}

/** Ligne de notification cliquable : icone de niveau, titre, message, date relative, pastille non lue. */
export function NotificationItem({ notification, onOpen, compact }: NotificationItemProps) {
  const Icon = ICONS[notification.niveau] ?? Info
  const niveau = describeStatus('niveauNotification', notification.niveau)
  return (
    <button
      type="button"
      onClick={() => {
        onOpen(notification)
      }}
      className={cn(
        'flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-muted focus-visible:bg-muted focus-visible:outline-none',
        !notification.lue && 'bg-primary/[0.04]',
      )}
    >
      <span
        className={cn(
          'mt-0.5 grid size-8 shrink-0 place-items-center rounded-lg',
          softToneClasses[niveau.tone],
        )}
      >
        <Icon className="size-4" aria-hidden />
        <span className="sr-only">{niveau.label}</span>
      </span>
      <span className="grid min-w-0 flex-1 gap-0.5">
        <span className="flex items-start justify-between gap-2">
          <span className={cn('text-sm', notification.lue ? 'font-medium' : 'font-semibold')}>
            {notification.titre}
          </span>
          {!notification.lue && (
            <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary">
              <span className="sr-only">Non lue</span>
            </span>
          )}
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
