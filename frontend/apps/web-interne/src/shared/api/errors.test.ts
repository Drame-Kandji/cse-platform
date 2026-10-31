import { describe, expect, it } from 'vitest'
import { ApiError, getErrorMessage, parseProblemDetail } from './errors'
import { createQueryKeys } from './query-keys'
import { shouldRetry } from './query-client'

describe('parseProblemDetail', () => {
  it('ignore les champs mal types', () => {
    expect(parseProblemDetail({ status: 'abc' })).toEqual({})
    expect(parseProblemDetail(null)).toEqual({})
  })
  it('conserve les champs connus', () => {
    expect(parseProblemDetail({ title: 'T', status: 404, code: 'X', extra: 1 })).toMatchObject({
      title: 'T',
      status: 404,
      code: 'X',
    })
  })
})

describe('getErrorMessage', () => {
  it('priorise le detail du ProblemDetail', () => {
    expect(getErrorMessage(new ApiError(409, { title: 'Conflit', detail: 'Version obsolète' }))).toBe(
      'Version obsolète',
    )
  })
  it('retombe sur le titre puis sur un message generique', () => {
    expect(getErrorMessage(new ApiError(404))).toBe('La ressource demandée est introuvable.')
    expect(getErrorMessage('???')).toBe('Une erreur inattendue est survenue.')
  })
})

describe('shouldRetry', () => {
  it('ne reessaie jamais une erreur 4xx', () => {
    expect(shouldRetry(0, new ApiError(400))).toBe(false)
    expect(shouldRetry(0, new ApiError(404))).toBe(false)
  })
  it('reessaie 2 fois les erreurs serveur et reseau', () => {
    expect(shouldRetry(0, new ApiError(503))).toBe(true)
    expect(shouldRetry(1, new ApiError(0))).toBe(true)
    expect(shouldRetry(2, new ApiError(500))).toBe(false)
  })
})

describe('createQueryKeys', () => {
  it('produit des cles hierarchiques', () => {
    const keys = createQueryKeys('besoins')
    expect(keys.all).toEqual(['besoins'])
    expect(keys.list({ page: 1 })).toEqual(['besoins', 'list', { page: 1 }])
    expect(keys.detail('b1')).toEqual(['besoins', 'detail', 'b1'])
    expect(keys.custom('historique', 'b1')).toEqual(['besoins', 'historique', 'b1'])
  })
})
