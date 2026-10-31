import { isApiError } from './errors'
import { send } from './http'

/** Message Server-Sent Events decode (format `text/event-stream`). */
export interface SseMessage {
  /** Nom de l'evenement (`message` si le champ `event` est absent). */
  readonly event: string
  /** Donnees : lignes `data:` jointes par un saut de ligne. */
  readonly data: string
  readonly id?: string
}

export interface SseParser {
  /** Injecte un fragment de texte recu (decoupage arbitraire). */
  feed: (chunk: string) => void
  /** Fin de flux : un evenement non termine par une ligne vide est abandonne (spec WHATWG). */
  end: () => void
}

/**
 * Analyseur incremental de flux `text/event-stream` (spec WHATWG) : gere les fragments coupes
 * au milieu d'une ligne, les fins de ligne CR/LF/CRLF, les donnees multi-lignes et les commentaires.
 * @param onMessage appele pour chaque evenement complet
 * @param onRetry appele quand le serveur impose un delai de reconnexion (`retry:`)
 */
export function createSseParser(
  onMessage: (message: SseMessage) => void,
  onRetry?: (delayMs: number) => void,
): SseParser {
  let buffer = ''
  let skipLeadingLf = false
  let event = ''
  let data: string[] = []
  let id: string | undefined

  const dispatch = () => {
    if (data.length > 0) {
      onMessage({ event: event || 'message', data: data.join('\n'), ...(id !== undefined ? { id } : {}) })
    }
    event = ''
    data = []
  }

  const processLine = (line: string) => {
    if (line === '') {
      dispatch()
      return
    }
    if (line.startsWith(':')) return
    const colon = line.indexOf(':')
    const field = colon === -1 ? line : line.slice(0, colon)
    let value = colon === -1 ? '' : line.slice(colon + 1)
    if (value.startsWith(' ')) value = value.slice(1)
    switch (field) {
      case 'event':
        event = value
        break
      case 'data':
        data.push(value)
        break
      case 'id':
        if (!value.includes('\0')) id = value
        break
      case 'retry':
        if (/^\d+$/.test(value)) onRetry?.(Number(value))
        break
      default:
        break
    }
  }

  return {
    feed(chunk) {
      let text = chunk
      if (skipLeadingLf && text.startsWith('\n')) text = text.slice(1)
      skipLeadingLf = false
      buffer += text
      let start = 0
      for (let index = 0; index < buffer.length; index += 1) {
        const char = buffer[index]
        if (char !== '\n' && char !== '\r') continue
        processLine(buffer.slice(start, index))
        if (char === '\r') {
          if (index + 1 === buffer.length) skipLeadingLf = true
          else if (buffer[index + 1] === '\n') index += 1
        }
        start = index + 1
      }
      buffer = buffer.slice(start)
    },
    end() {
      buffer = ''
      event = ''
      data = []
    },
  }
}

export interface SseConnectionOptions {
  /** Chemin relatif a la passerelle, ex. `/notifications/stream`. */
  path: string
  onMessage: (message: SseMessage) => void
  /** Connexion (re)etablie. */
  onOpen?: () => void
  /** Erreur de connexion (une reconnexion suit, sauf 401/403). */
  onError?: (error: unknown) => void
  /** Delai initial de reconnexion (ms), double a chaque echec. */
  initialDelayMs?: number
  /** Plafond du delai de reconnexion (ms). */
  maxDelayMs?: number
}

const wait = (ms: number, signal: AbortSignal) =>
  new Promise<void>((resolve) => {
    if (signal.aborted) {
      resolve()
      return
    }
    const timer = window.setTimeout(done, ms)
    function done() {
      window.clearTimeout(timer)
      signal.removeEventListener('abort', done)
      resolve()
    }
    signal.addEventListener('abort', done)
  })

/**
 * Ouvre un flux SSE authentifie via `fetch` (en-tete `Authorization` fourni par la couche HTTP,
 * jeton rafraichi a chaque reconnexion) avec reconnexion a backoff exponentiel plafonne.
 * S'arrete definitivement sur 401/403 ou a l'appel de la fonction retournee.
 * @returns fonction d'arret (deconnexion, demontage)
 * @example const stop = connectSse({ path: '/notifications/stream', onMessage }); // ... stop()
 */
export function connectSse(options: SseConnectionOptions): () => void {
  const { path, onMessage, onOpen, onError, initialDelayMs = 1_000, maxDelayMs = 30_000 } = options
  const controller = new AbortController()
  const { signal } = controller
  let baseDelay = initialDelayMs
  // Fonction (et non lecture directe) : l'etat change pendant les attentes asynchrones.
  const stopped = () => signal.aborted

  const run = async () => {
    let attempt = 0
    while (!stopped()) {
      try {
        const response = await send(path, {
          headers: { Accept: 'text/event-stream', 'Cache-Control': 'no-cache' },
          signal,
          responseType: 'text',
        })
        if (!response.body) throw new Error('Flux SSE indisponible')
        attempt = 0
        onOpen?.()
        const parser = createSseParser(onMessage, (delay) => {
          baseDelay = delay
        })
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        for (;;) {
          const { value, done } = await reader.read()
          if (done) break
          parser.feed(decoder.decode(value, { stream: true }))
        }
        parser.end()
      } catch (error) {
        if (stopped()) return
        onError?.(error)
        if (isApiError(error) && (error.status === 401 || error.status === 403)) return
      }
      if (stopped()) return
      const delay = Math.min(maxDelayMs, baseDelay * 2 ** attempt)
      attempt += 1
      await wait(delay * (0.8 + Math.random() * 0.4), signal)
    }
  }
  void run()
  return () => {
    controller.abort()
  }
}
