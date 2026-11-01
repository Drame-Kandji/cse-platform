import { mkdirSync } from 'node:fs'
import { chromium, expect, type FullConfig } from '@playwright/test'
import { comptes, comptesPortail, e2eEnv, type RoleE2e, storageStateOf } from './support/env'
import { connecterPortail } from './support/portail'

/**
 * Configuration globale : connexion de chaque rôle UNE fois via la vraie page de connexion Keycloak,
 * puis sauvegarde du storageState (cookies de session SSO Keycloak) dans `e2e/.auth/<role>.json`.
 * Les tests réutilisent ces sessions : keycloak-js les retrouve par `check-sso` silencieux.
 * Le fournisseur se connecte de même sur le portail (origine distincte, `e2e/.auth/fournisseur.json`).
 */
export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0]?.use.baseURL ?? e2eEnv.baseURL
  mkdirSync('e2e/.auth', { recursive: true })
  const browser = await chromium.launch()
  try {
    for (const role of Object.keys(comptes) as RoleE2e[]) {
      const context = await browser.newContext({ baseURL, locale: 'fr-FR' })
      const page = await context.newPage()
      await page.goto('/')
      await page.getByRole('button', { name: 'Se connecter avec le compte CSE' }).click()
      await page.getByRole('textbox', { name: "Nom d'utilisateur ou courriel" }).fill(comptes[role])
      await page.getByRole('textbox', { name: 'Mot de passe' }).fill(e2eEnv.password)
      await page.getByRole('button', { name: 'Connexion' }).click()
      // Retour dans l'application : la coquille affiche la navigation principale.
      await expect(page.getByRole('navigation').first()).toBeVisible({ timeout: 30_000 })
      await context.storageState({ path: storageStateOf(role) })
      await context.close()
    }
    const context = await browser.newContext({ baseURL: e2eEnv.portailURL, locale: 'fr-FR' })
    const page = await context.newPage()
    await connecterPortail(page, comptesPortail.fournisseur)
    await context.storageState({ path: storageStateOf('fournisseur') })
    await context.close()
  } finally {
    await browser.close()
  }
}
