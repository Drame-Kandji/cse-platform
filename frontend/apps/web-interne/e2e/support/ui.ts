import { expect, type Locator, type Page } from '@playwright/test'

/** Suffixe unique pour rendre les données de test identifiables et les tests rejouables. */
export const suffixeUnique = () =>
  `${Date.now().toString(36).toUpperCase()}${Math.floor(Math.random() * 1296)
    .toString(36)
    .toUpperCase()}`

/** Date ISO (aaaa-mm-jj) à J+n dans le fuseau local. */
export function dateIso(joursDepuisAujourdhui: number): string {
  const d = new Date()
  d.setDate(d.getDate() + joursDepuisAujourdhui)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const jj = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${jj}`
}

const NOM_JOUR = new Intl.DateTimeFormat('fr-FR', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

/**
 * Choisit une date dans un DatePicker du design system : ouvre le calendrier (bouton nommé par le
 * libellé du champ), avance de mois si besoin et clique la cellule « samedi 31 octobre 2026 ».
 */
export async function choisirDate(page: Page, champ: Locator, iso: string) {
  await champ.click()
  const calendrier = page.getByRole('dialog').filter({ has: page.getByRole('grid') })
  const [annee, mois, jour] = iso.split('-').map(Number) as [number, number, number]
  const nom = NOM_JOUR.format(new Date(annee, mois - 1, jour))
  const cellule = calendrier.getByRole('gridcell', { name: nom, exact: true })
  for (let i = 0; i < 24 && !(await cellule.isVisible()); i++) {
    await calendrier.getByRole('button', { name: 'Mois suivant' }).click()
  }
  await cellule.click()
  await expect(calendrier).toBeHidden()
}

/** Sélectionne une option d'un Select (Radix) identifié par son nom accessible. */
export async function choisirOption(page: Page, select: Locator, option: string | RegExp) {
  await select.click()
  await page.getByRole('option', { name: option }).click()
}

/** Toast sonner affiché (région « Notifications alt+T »). */
export function toast(page: Page, texte: string | RegExp) {
  return page
    .getByRole('region', { name: /^Notifications alt/ })
    .getByText(texte)
    .first()
}

/** Attend l'affichage d'un toast de succès. */
export async function attendreToast(page: Page, texte: string | RegExp) {
  await expect(toast(page, texte)).toBeVisible()
}
