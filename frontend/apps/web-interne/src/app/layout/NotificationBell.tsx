import { Bell } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { NotificationPopoverPanel } from '@/features/notifications/components/NotificationPopoverPanel'
import { useUnreadCount } from '@/features/notifications/hooks/useNotifications'
import { useNotificationStream } from '@/features/notifications/hooks/useNotificationStream'
import { Button } from '@/shared/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/ui/popover'

/**
 * Cloche de notifications : pastille des non lues, popover des 10 dernieres et
 * abonnement temps reel (SSE) actif tant que la coquille est montee.
 */
export function NotificationBell() {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  useNotificationStream()
  const unreadCount = useUnreadCount().data?.count ?? 0
  const close = useCallback(() => {
    setOpen(false)
  }, [])
  const label =
    unreadCount > 0 ? `${t('shell.notifications')} (${unreadCount} non lues)` : t('shell.notifications')
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={label} className="relative">
          <Bell />
          {unreadCount > 0 && (
            <span
              aria-hidden
              className="pointer-events-none absolute right-1.5 top-1.5 grid min-w-4 place-items-center rounded-full bg-primary px-1 text-[10px] font-semibold leading-4 text-primary-foreground ring-2 ring-surface"
            >
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(24rem,calc(100vw-1rem))] p-0">
        {open && <NotificationPopoverPanel unreadCount={unreadCount} onClose={close} />}
      </PopoverContent>
    </Popover>
  )
}
