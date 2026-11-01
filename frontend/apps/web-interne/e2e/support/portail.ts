import { expect, type Page } from '@playwright/test'
import { e2eEnv } from './env'

/**
 * Ouvre le portail fournisseur et s'y connecte si besoin (page « Portail fournisseur » puis formulaire
 * Keycloak). Avec une session SSO déjà présente, keycloak-js la retrouve sans formulaire.
 */
export async function connecterPortail(page: Page, email: string) {
  await page.goto(e2eEnv.portailURL + '/')
  const menu = page.getByRole('navigation').first()
  const bouton = page.getByRole('button', { name: 'Se connecter', exact: true })
  await expect(menu.or(bouton)).toBeVisible({ timeout: 30_000 })
  if (await bouton.isVisible()) {
    await bouton.click()
    await page.getByRole('textbox', { name: "Nom d'utilisateur ou courriel" }).fill(email)
    await page.getByRole('textbox', { name: 'Mot de passe' }).fill(e2eEnv.password)
    await page.getByRole('button', { name: 'Connexion' }).click()
  }
  await expect(menu).toBeVisible({ timeout: 30_000 })
}
