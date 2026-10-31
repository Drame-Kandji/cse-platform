import { http as mswHttp, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { downloadFile } from './download'
import { ApiError } from './errors'
import { buildUrl, configureHttp, http } from './http'

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

const onUnauthorized = vi.fn()
beforeEach(() => {
  onUnauthorized.mockReset()
  configureHttp({ baseUrl: '/api', getToken: () => Promise.resolve('jeton-123'), onUnauthorized })
})

describe('http', () => {
  it('ajoute le jeton Bearer, un X-Correlation-Id et parse le JSON', async () => {
    let headers: Headers | undefined
    server.use(
      mswHttp.get(`${API}/materiels/42`, ({ request }) => {
        headers = request.headers
        return HttpResponse.json({ id: '42', designation: 'Pelle' })
      }),
    )
    const result = await http.get<{ id: string; designation: string }>('/materiels/42')
    expect(result).toEqual({ id: '42', designation: 'Pelle' })
    expect(headers?.get('authorization')).toBe('Bearer jeton-123')
    expect(headers?.get('x-correlation-id')).toMatch(/^[0-9a-f-]{16,}$/i)
  })

  it("n'envoie pas d'en-tete Authorization sans jeton", async () => {
    configureHttp({ getToken: () => Promise.resolve(null) })
    let auth: string | null = 'initial'
    server.use(
      mswHttp.get(`${API}/ping`, ({ request }) => {
        auth = request.headers.get('authorization')
        return HttpResponse.json({ ok: true })
      }),
    )
    await http.get('/ping')
    expect(auth).toBeNull()
  })

  it('serialise le corps JSON et gere une reponse 204', async () => {
    let body: unknown
    server.use(
      mswHttp.post(`${API}/besoins`, async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    await expect(http.post('/besoins', { quantite: 2 })).resolves.toBeUndefined()
    expect(body).toEqual({ quantite: 2 })
  })

  it('transforme un ProblemDetail RFC 7807 en ApiError', async () => {
    server.use(
      mswHttp.post(`${API}/besoins`, () =>
        HttpResponse.json(
          {
            type: 'https://cse.sn/problemes/validation',
            title: 'Données invalides',
            status: 422,
            detail: 'La quantité doit être positive.',
            code: 'BESOIN_INVALIDE',
            traceId: 'trace-abc',
            errors: [{ field: 'quantite', message: 'doit être positive' }],
          },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const error = await http.post('/besoins', { quantite: -1 }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    const apiError = error as ApiError
    expect(apiError.status).toBe(422)
    expect(apiError.code).toBe('BESOIN_INVALIDE')
    expect(apiError.title).toBe('Données invalides')
    expect(apiError.userMessage).toBe('La quantité doit être positive.')
    expect(apiError.traceId).toBe('trace-abc')
    expect(apiError.fieldErrors).toEqual([{ field: 'quantite', message: 'doit être positive' }])
    expect(apiError.isClientError).toBe(true)
    expect(apiError.correlationId).toBeTruthy()
  })

  it('fournit un message francais par defaut si le corps est vide', async () => {
    server.use(mswHttp.get(`${API}/x`, () => new HttpResponse('oops', { status: 503 })))
    const error = (await http.get('/x').catch((e: unknown) => e)) as ApiError
    expect(error.status).toBe(503)
    expect(error.userMessage).toBe('Le service est momentanément indisponible.')
  })

  it('appelle onUnauthorized sur 401', async () => {
    server.use(mswHttp.get(`${API}/secure`, () => new HttpResponse(null, { status: 401 })))
    await expect(http.get('/secure')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('convertit une erreur reseau en ApiError status 0', async () => {
    server.use(mswHttp.get(`${API}/down`, () => HttpResponse.error()))
    const error = (await http.get('/down').catch((e: unknown) => e)) as ApiError
    expect(error.status).toBe(0)
    expect(error.isNetworkError).toBe(true)
    expect(error.code).toBe('NETWORK_ERROR')
  })

  it('construit la query string en ignorant les valeurs vides et en repetant les tableaux', () => {
    const url = new URL(
      buildUrl('/besoins', {
        page: 0,
        statut: ['EN_ATTENTE', 'VALIDE'],
        q: '',
        chantier: undefined,
        urgent: true,
      }),
    )
    expect(url.pathname).toBe('/api/besoins')
    expect(url.searchParams.getAll('statut')).toEqual(['EN_ATTENTE', 'VALIDE'])
    expect(url.searchParams.get('page')).toBe('0')
    expect(url.searchParams.has('q')).toBe(false)
    expect(url.searchParams.has('chantier')).toBe(false)
    expect(url.searchParams.get('urgent')).toBe('true')
  })

  it('telecharge un fichier et lit le nom depuis Content-Disposition', async () => {
    server.use(
      mswHttp.get(
        `${API}/rapports/parc`,
        () =>
          new HttpResponse('contenu', {
            headers: {
              'Content-Type': 'application/pdf',
              'Content-Disposition': "attachment; filename*=UTF-8''rapport%20parc.pdf",
            },
          }),
      ),
    )
    const file = await downloadFile('/rapports/parc', 'defaut.pdf')
    expect(file.filename).toBe('rapport parc.pdf')
    expect(await file.blob.text()).toBe('contenu')
  })
})
