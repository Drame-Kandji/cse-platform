import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationsApi } from '../api/notifications.api'
import { notificationKeys, unreadCountKey } from '../api/notifications.keys'
import { adjustUnreadCount, markReadInCache } from '../lib/cache'
import type { Notification, NotificationFiltres } from '../types'

/** Liste paginee des notifications de l'utilisateur. */
export function useNotifications(params: NotificationFiltres, options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: notificationKeys.list(params),
    queryFn: ({ signal }) => notificationsApi.lister(params, signal),
    placeholderData: keepPreviousData,
    enabled: options.enabled ?? true,
  })
}

/** Nombre de notifications non lues (rafraichi toutes les 2 minutes en secours du flux SSE). */
export function useUnreadCount() {
  return useQuery({
    queryKey: unreadCountKey,
    queryFn: ({ signal }) => notificationsApi.nonLues(signal),
    refetchInterval: 120_000,
  })
}

/** Marque une notification comme lue (mise a jour immediate du cache et du compteur). */
export function useMarquerLue() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (notification: Notification) => notificationsApi.marquerLue(notification.id),
    meta: { silentError: true },
    onMutate: (notification) => {
      if (notification.lue) return
      markReadInCache(queryClient, [notification.id])
      adjustUnreadCount(queryClient, (count) => count - 1)
    },
    onError: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  })
}

/** Marque toutes les notifications comme lues. */
export function useToutLire() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationsApi.toutLire,
    meta: { successMessage: 'Toutes les notifications sont marquées comme lues' },
    onMutate: () => {
      markReadInCache(queryClient, 'all')
      adjustUnreadCount(queryClient, () => 0)
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  })
}
