import { http as mswHttp, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { configureHttp } from './http'
import { connectSse, createSseParser, type SseMessage } from './sse'

function collect() {
  const messages: SseMessage[] = []
  const parser = createSseParser((message) => messages.push(message))
  return { messages, parser }
}

describe('createSseParser', () => {
  it('decode un evenement nomme avec ses donnees JSON', () => {
    const { messages, parser } = collect()
    parser.feed('event: notification\ndata: {"id":"n1"}\n\n')
    expect(messages).toEqual([{ event: 'notification', data: '{"id":"n1"}' }])
  })

  it('reconstitue un evenement coupe a des frontieres arbitraires', () => {
    const { messages, parser } = collect()
    const raw = 'event: notification\ndata: {"titre":"Besoin valide"}\n\nevent: ping\ndata: ok\n\n'
    for (const char of raw) parser.feed(char)
    expect(messages).toEqual([
      { event: 'notification', data: '{"titre":"Besoin valide"}' },
      { event: 'ping', data: 'ok' },
    ])
  })

  it('gere un CRLF coupe entre deux fragments', () => {
    const { messages, parser } = collect()
    parser.feed('data: a\r')
    parser.feed('\n\r')
    parser.feed('\n')
    expect(messages).toEqual([{ event: 'message', data: 'a' }])
  })

  it('joint les lignes data multiples par un saut de ligne', () => {
    const { messages, parser } = collect()
    parser.feed('data: ligne 1\ndata: ligne 2\ndata:ligne 3\n\n')
    expect(messages[0]?.data).toBe('ligne 1\nligne 2\nligne 3')
  })

  it('ignore les commentaires et les blocs sans donnees', () => {
    const { messages, parser } = collect()
    parser.feed(': keep-alive\n\nevent: ping\n\n')
    expect(messages).toEqual([])
  })

  it("abandonne un evenement incomplet en fin de flux et lit l'identifiant", () => {
    const { messages, parser } = collect()
    parser.feed('id: 42\ndata: x\n\ndata: incomplet')
    parser.end()
    expect(messages).toEqual([{ event: 'message', data: 'x', id: '42' }])
  })

  it('remonte le delai retry impose par le serveur', () => {
    const onRetry = vi.fn()
    const parser = createSseParser(() => undefined, onRetry)
    parser.feed('retry: 5000\nretry: abc\n')
    expect(onRetry).toHaveBeenCalledOnce()
    expect(onRetry).toHaveBeenCalledWith(5000)
  })
})

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
beforeEach(() => {
  configureHttp({ baseUrl: '/api', getToken: () => Promise.resolve('jeton-sse') })
})

describe('connectSse', () => {
  it('envoie le jeton Bearer et diffuse les evenements du flux', async () => {
    let auth: string | null = null
    server.use(
      mswHttp.get(`${API}/notifications/stream`, ({ request }) => {
        auth = request.headers.get('authorization')
        const encoder = new TextEncoder()
        const stream = new ReadableStream<Uint8Array>({
          start(controller) {
            controller.enqueue(encoder.encode('event: notification\ndata: {"id"'))
            controller.enqueue(encoder.encode(':"n1"}\n\n'))
          },
        })
        return new HttpResponse(stream, { headers: { 'Content-Type': 'text/event-stream' } })
      }),
    )
    const onMessage = vi.fn()
    const stop = connectSse({ path: '/notifications/stream', onMessage })
    await vi.waitFor(() => {
      expect(onMessage).toHaveBeenCalledWith({ event: 'notification', data: '{"id":"n1"}' })
    })
    stop()
    expect(auth).toBe('Bearer jeton-sse')
  })

  it("s'arrete sans reconnexion sur 401", async () => {
    let calls = 0
    server.use(
      mswHttp.get(`${API}/notifications/stream`, () => {
        calls += 1
        return HttpResponse.json({ status: 401, title: 'Non authentifié' }, { status: 401 })
      }),
    )
    const onError = vi.fn()
    const stop = connectSse({ path: '/notifications/stream', onMessage: vi.fn(), onError, initialDelayMs: 1 })
    await vi.waitFor(() => {
      expect(onError).toHaveBeenCalledOnce()
    })
    await new Promise((resolve) => setTimeout(resolve, 20))
    expect(calls).toBe(1)
    stop()
  })
})
