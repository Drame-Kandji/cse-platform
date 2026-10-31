import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { useLocation } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { notificationKeys, unreadCountKey } from '../api/notifications.keys'
import { notificationFromMessage } from '../hooks/useNotificationStream'
import { prependNotification } from '../lib/cache'
import type { Notification } from '../types'
import { LATEST_NOTIFICATIONS, NotificationPopoverPanel } from './NotificationPopoverPanel'

const API = `${window.location.origin}/api`
const server = setupServer()
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

const notification = (overrides: Partial<Notification> = {}): Notification => ({
  id: 'n-1',
  type: 'BESOIN_VALIDE',
  titre: 'Besoin validé',
  message: 'Le besoin BES-2026-0001 a été validé.',
  niveau: 'SUCCES',
  lien: '/besoins/b-1',
  lue: false,
  createdAt: new Date().toISOString(),
  ...overrides,
})

const page = (content: Notification[]) => ({
  content,
  page: 0,
  size: 10,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})

function Location() {
  return <p data-testid="location">{useLocation().pathname}</p>
}

describe('NotificationPopoverPanel', () => {
  it('marque la notification comme lue puis navigue vers son lien', async () => {
    const patched = vi.fn()
    server.use(
      http.get(`${API}/notifications`, () => HttpResponse.json(page([notification()]))),
      http.patch(`${API}/notifications/n-1/lue`, () => {
        patched()
        return HttpResponse.json(notification({ lue: true }))
      }),
    )
    const onClose = vi.fn()
    const user = userEvent.setup()
    const { queryClient } = renderWithProviders(
      <>
        <NotificationPopoverPanel unreadCount={1} onClose={onClose} />
        <Location />
      </>,
    )
    queryClient.setQueryData(unreadCountKey, { count: 1 })
    await user.click(await screen.findByRole('button', { name: /Besoin validé/ }))
    expect(screen.getByTestId('location')).toHaveTextContent('/besoins/b-1')
    expect(onClose).toHaveBeenCalled()
    await waitFor(() => {
      expect(patched).toHaveBeenCalledOnce()
    })
    expect(queryClient.getQueryData(unreadCountKey)).toEqual({ count: 0 })
  })
})

describe('flux temps reel', () => {
  it('ignore les ping et decode les notifications', () => {
    expect(notificationFromMessage({ event: 'ping', data: 'ok' })).toBeNull()
    expect(notificationFromMessage({ event: 'notification', data: '{oops' })).toBeNull()
    expect(notificationFromMessage({ event: 'notification', data: JSON.stringify(notification()) })?.id).toBe(
      'n-1',
    )
  })

  it('insere en tete du cache et incremente le compteur une seule fois', () => {
    const { queryClient } = renderWithProviders(<span />)
    queryClient.setQueryData(
      notificationKeys.list(LATEST_NOTIFICATIONS),
      page([notification({ id: 'ancienne', lue: true })]),
    )
    queryClient.setQueryData(unreadCountKey, { count: 2 })
    prependNotification(queryClient, notification({ id: 'n-2', niveau: 'CRITIQUE' }))
    prependNotification(queryClient, notification({ id: 'n-2', niveau: 'CRITIQUE' }))
    const data = queryClient.getQueryData<ReturnType<typeof page>>(
      notificationKeys.list(LATEST_NOTIFICATIONS),
    )
    expect(data?.content.map((item) => item.id)).toEqual(['n-2', 'ancienne'])
    expect(queryClient.getQueryData(unreadCountKey)).toEqual({ count: 3 })
  })
})
