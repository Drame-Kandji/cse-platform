import { http } from '@/shared/api/http'
import type { PageResponse } from '@/shared/api/types'
import type { NonLuesCount, Notification, NotificationFiltres } from '../types'

/** Chemin du flux SSE (relatif a la passerelle). */
export const NOTIFICATIONS_STREAM_PATH = '/notifications/stream'

export const notificationsApi = {
  lister: (params: NotificationFiltres, signal?: AbortSignal) =>
    http.get<PageResponse<Notification>>('/notifications', { query: { ...params }, signal }),
  nonLues: (signal?: AbortSignal) => http.get<NonLuesCount>('/notifications/non-lues/count', { signal }),
  marquerLue: (id: string) => http.patch<Notification>(`/notifications/${encodeURIComponent(id)}/lue`),
  toutLire: () => http.post<undefined>('/notifications/tout-lire', undefined, { responseType: 'void' }),
}
