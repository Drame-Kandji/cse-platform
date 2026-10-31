import type { QueryClient } from '@tanstack/react-query'
import type { PageResponse } from '@/shared/api/types'
import { notificationKeys, unreadCountKey } from '../api/notifications.keys'
import type { NonLuesCount, Notification, NotificationFiltres } from '../types'

type NotificationPage = PageResponse<Notification>

function paramsOf(queryKey: readonly unknown[]): NotificationFiltres {
  const params = queryKey[2]
  return typeof params === 'object' && params !== null ? params : {}
}

/**
 * Insere une notification recue en direct en tete des listes en cache (premiere page,
 * filtre compatible) et incremente le compteur de non lues. Sans effet si deja presente.
 */
export function prependNotification(queryClient: QueryClient, notification: Notification): void {
  let alreadyKnown = false
  for (const [queryKey, data] of queryClient.getQueriesData<NotificationPage>({
    queryKey: notificationKeys.lists(),
  })) {
    if (!data) continue
    if (data.content.some((item) => item.id === notification.id)) {
      alreadyKnown = true
      continue
    }
    const params = paramsOf(queryKey)
    if ((params.page ?? 0) !== 0) continue
    if (params.lue !== undefined && params.lue !== notification.lue) continue
    const size = params.size ?? data.size
    queryClient.setQueryData<NotificationPage>(queryKey, {
      ...data,
      content: [notification, ...data.content].slice(0, size),
      totalElements: data.totalElements + 1,
      totalPages: Math.max(1, Math.ceil((data.totalElements + 1) / Math.max(1, size))),
    })
  }
  if (!alreadyKnown && !notification.lue) {
    queryClient.setQueryData<NonLuesCount>(unreadCountKey, (current) =>
      current ? { count: current.count + 1 } : current,
    )
  }
}

/** Marque des notifications comme lues dans toutes les listes en cache (mise a jour optimiste). */
export function markReadInCache(queryClient: QueryClient, ids: readonly string[] | 'all'): number {
  let changed = 0
  const matches = (item: Notification) => !item.lue && (ids === 'all' || ids.includes(item.id))
  queryClient.setQueriesData<NotificationPage>({ queryKey: notificationKeys.lists() }, (data) => {
    if (!data?.content.some(matches)) return data
    changed += data.content.filter(matches).length
    return { ...data, content: data.content.map((item) => (matches(item) ? { ...item, lue: true } : item)) }
  })
  return changed
}

/** Ajuste le compteur de non lues (borne a 0). */
export function adjustUnreadCount(queryClient: QueryClient, update: (count: number) => number): void {
  queryClient.setQueryData<NonLuesCount>(unreadCountKey, (current) =>
    current ? { count: Math.max(0, update(current.count)) } : current,
  )
}
