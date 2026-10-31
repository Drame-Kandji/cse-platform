import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Notification } from '../types'
import { useMarquerLue } from './useNotifications'

/**
 * Ouvre une notification : la marque comme lue (si besoin) puis navigue vers son `lien`
 * (route interne uniquement).
 * @param onNavigate appele avant la navigation (ex. fermer le popover)
 */
export function useOpenNotification(onNavigate?: () => void) {
  const navigate = useNavigate()
  const { mutate } = useMarquerLue()
  return useCallback(
    (notification: Notification) => {
      if (!notification.lue) mutate(notification)
      const lien = notification.lien
      if (lien?.startsWith('/') && !lien.startsWith('//')) {
        onNavigate?.()
        navigate(lien)
      }
    },
    [mutate, navigate, onNavigate],
  )
}
