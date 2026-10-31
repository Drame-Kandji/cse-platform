import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { useAssistantChat } from '../hooks/useAssistant'
import type { Question, Reponse } from '../types'
import { AssistantChat } from './AssistantChat'

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

function Harness() {
  const chat = useAssistantChat()
  return <AssistantChat chat={chat} firstName="Ibrahima" />
}

const reponse: Reponse = {
  conversationId: 'c-1',
  messageId: 'm-1',
  reponse: 'Il y a **2 pelles** disponibles.',
  sources: [
    { type: 'MATERIEL', id: 'mat-1', libelle: 'PEL-001 Pelle hydraulique', lien: '/materiels/mat-1' },
  ],
  intention: 'DISPONIBILITE',
  modeDegrade: true,
  horodatage: '2026-10-30T09:15:00Z',
}

describe('AssistantChat', () => {
  it('envoie la question a Entree et affiche la reponse, ses sources et le mode degrade', async () => {
    let body: Question | undefined
    server.use(
      http.get(`${API}/chatbot/suggestions`, () => HttpResponse.json(['Quels engins sont disponibles ?'])),
      http.post(`${API}/chatbot/query`, async ({ request }) => {
        body = (await request.json()) as Question
        return HttpResponse.json(reponse)
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<Harness />)
    expect(await screen.findByRole('button', { name: 'Quels engins sont disponibles ?' })).toBeInTheDocument()

    const input = screen.getByLabelText('Votre question')
    await user.type(input, 'Pelles disponibles ?{Shift>}{Enter}{/Shift}à Dakar')
    expect(input).toHaveValue('Pelles disponibles ?\nà Dakar')
    expect(screen.getByText(`${'Pelles disponibles ?\nà Dakar'.length}/1000`)).toBeInTheDocument()
    await user.keyboard('{Enter}')

    expect(await screen.findByText('Mode dégradé')).toBeInTheDocument()
    expect(body).toEqual({ question: 'Pelles disponibles ?\nà Dakar' })
    expect(screen.getByText('2 pelles').tagName).toBe('STRONG')
    expect(screen.getByRole('link', { name: /PEL-001 Pelle hydraulique/ })).toHaveAttribute(
      'href',
      '/materiels/mat-1',
    )
    expect(input).toHaveValue('')
  })

  it("affiche l'erreur en ligne sous la question et conserve la conversation", async () => {
    server.use(
      http.get(`${API}/chatbot/suggestions`, () => HttpResponse.json([])),
      http.post(`${API}/chatbot/query`, () =>
        HttpResponse.json(
          { status: 503, title: 'Service indisponible', detail: 'Assistant momentanément indisponible.' },
          { status: 503 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<Harness />)
    await user.type(screen.getByLabelText('Votre question'), 'Bonjour{Enter}')
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Assistant momentanément indisponible.')
    })
    expect(screen.getByText('Bonjour')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument()
  })
})
