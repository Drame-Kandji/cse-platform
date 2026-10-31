import { createQueryKeys } from '@/shared/api/query-keys'

export const notificationKeys = createQueryKeys('notifications')
/** Cle du compteur de non lues (pastille de la cloche). */
export const unreadCountKey = notificationKeys.custom('non-lues')
