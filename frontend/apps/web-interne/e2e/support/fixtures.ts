import { test as base, expect, type Page } from '@playwright/test'
import { type RoleE2e, storageStateOf } from './env'

interface Fixtures {
  /**
   * Ouvre (ou réutilise) une page connectée sous un rôle : un contexte navigateur par rôle,
   * initialisé avec la session Keycloak sauvegardée par la configuration globale.
   */
  en: (role: RoleE2e) => Promise<Page>
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
})

export { expect }

/** Navigue vers une route et attend que la coquille et le titre de page soient affichés. */
export async function aller(page: Page, chemin: string) {
  await page.goto(chemin)
  await expect(page.getByRole('main').getByRole('heading', { level: 1 }).first()).toBeVisible()
}
