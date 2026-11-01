import { test as base, expect, type Page } from '@playwright/test'
import { comptesPortail, e2eEnv, type RoleE2e, storageStateOf } from './env'
import { connecterPortail } from './portail'

interface Fixtures {
  /**
   * Ouvre (ou réutilise) une page connectée sous un rôle : un contexte navigateur par rôle,
   * initialisé avec la session Keycloak sauvegardée par la configuration globale.
   */
  en: (role: RoleE2e) => Promise<Page>
  /**
   * Page du portail fournisseur (http://localhost:5174) connectée sous le compte du fournisseur F1,
   * dans son propre contexte navigateur (origine distincte de l'application interne).
   */
  portail: () => Promise<Page>
}

export const test = base.extend<Fixtures>({
  en: async ({ browser }, fournir) => {
    const pages = new Map<RoleE2e, Page>()
    await fournir(async (role) => {
      const existante = pages.get(role)
      if (existante) return existante
      const context = await browser.newContext({ storageState: storageStateOf(role) })
      const page = await context.newPage()
      pages.set(role, page)
      return page
    })
    for (const page of pages.values()) await page.context().close()
  },
  portail: async ({ browser }, fournir) => {
    let page: Page | undefined
    await fournir(async () => {
      if (page) return page
      const context = await browser.newContext({
        baseURL: e2eEnv.portailURL,
        storageState: storageStateOf('fournisseur'),
      })
      page = await context.newPage()
      await connecterPortail(page, comptesPortail.fournisseur)
      return page
    })
    if (page) await page.context().close()
  },
})

export { expect }

/** Navigue vers une route et attend que la coquille et le titre de page soient affichés. */
export async function aller(page: Page, chemin: string) {
  await page.goto(chemin)
  await expect(page.getByRole('main').getByRole('heading', { level: 1 }).first()).toBeVisible()
}
