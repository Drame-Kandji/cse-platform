import { api, http, HttpResponse, page, problem, server } from '@/test/server'
import { portailApi } from './portail.api'

describe('portailApi', () => {
  it('liste tolerante : un 404 devient une page vide', async () => {
    server.use(http.get(api('/contrats'), () => problem(404, 'NOT_FOUND')))
    await expect(portailApi.contrats()).resolves.toMatchObject({ content: [], totalElements: 0 })
  })
  it('les autres erreurs remontent', async () => {
    server.use(http.get(api('/factures'), () => problem(500, 'ERREUR')))
    await expect(portailApi.factures()).rejects.toMatchObject({ status: 500 })
  })
  it('demande une page de 50 triee par date limite', async () => {
    let url = ''
    server.use(
      http.get(api('/appels-offres'), ({ request }) => {
        url = request.url
        return HttpResponse.json(page([]))
      }),
    )
    await portailApi.appelsOffres()
    expect(url).toContain('size=50')
    expect(url).toContain('sort=dateLimite%2Casc')
  })
})
