import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { connectSse, type SseMessage } from '@/shared/api/sse'
import { useAuth } from '@/shared/auth/context'
import { NOTIFICATIONS_STREAM_PATH } from '../api/notifications.api'
import { notificationKeys } from '../api/notifications.keys'
import { prependNotification } from '../lib/cache'
import type { Notification } from '../types'

/** Evenement SSE portant une notification (les `ping` de maintien de connexion sont ignores). */
export const NOTIFICATION_EVENT = 'notification'

/**
 * Decode un message SSE en notification ; `null` pour un ping, un autre evenement ou un JSON invalide.
 */
export function notificationFromMessage(message: SseMessage): Notification | null {
  if (message.event !== NOTIFICATION_EVENT) return null
  try {
    const value: unknown = JSON.parse(message.data)
    if (typeof value === 'object' && value !== null && 'id' in value && 'titre' in value) {
      return value as Notification
    }
  } catch {
    console.warn('Notification SSE illisible ignoree')
  }
  return null
}

function usePageVisible(): boolean {
  const [visible, setVisible] = useState(() => document.visibilityState !== 'hidden')
  useEffect(() => {
    const onChange = () => {
      setVisible(document.visibilityState !== 'hidden')
    }
    document.addEventListener('visibilitychange', onChange)
    return () => {
      document.removeEventListener('visibilitychange', onChange)
    }
  }, [])
  return visible
}

/**
 * Abonnement temps reel aux notifications (SSE via fetch, jeton de la session).
 * Insere chaque notification en tete du cache, incremente la pastille et affiche un toast
 * pour les niveaux ALERTE et CRITIQUE. Coupe a la deconnexion et quand l'onglet est masque ;
 * a la reprise, les listes sont resynchronisees.
 */
export function useNotificationStream(): void {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { status, user } = useAuth()
  const visible = usePageVisible()
  const navigateRef = useRef(navigate)
  navigateRef.current = navigate
  const connectedBefore = useRef(false)
  const userId = status === 'authenticated' ? user?.id : undefined

  useEffect(() => {
    if (!userId || !visible) return
    const stop = connectSse({
      path: NOTIFICATIONS_STREAM_PATH,
      onOpen: () => {
        // Reconnexion (coupure reseau, onglet de nouveau visible) : resynchroniser ce qui a ete manque.
        if (connectedBefore.current) void queryClient.invalidateQueries({ queryKey: notificationKeys.all })
        connectedBefore.current = true
      },
      onMessage: (message) => {
        const notification = notificationFromMessage(message)
        if (!notification) return
        prependNotification(queryClient, notification)
        if (notification.niveau === 'ALERTE' || notification.niveau === 'CRITIQUE') {
          const show = notification.niveau === 'CRITIQUE' ? toast.error : toast.warning
          const lien = notification.lien
          show(notification.titre, {
            description: notification.message,
            action: lien?.startsWith('/')
              ? {
                  label: 'Voir',
                  onClick: () => {
                    navigateRef.current(lien)
                  },
                }
              : undefined,
          })
        }
      },
    })
    return stop
  }, [userId, visible, queryClient])
}
