import { expect, type Page } from '@playwright/test'
import { attendreToast } from '../support/ui'

/** Navigue dans le portail fournisseur et attend le titre de page. */
async function allerPortail(page: Page, chemin: string) {
  await page.goto(chemin)
  await expect(page.getByRole('main').getByRole('heading', { level: 1 }).first()).toBeVisible()
}

/**
 * Portail fournisseur (http://localhost:5174) : dépôt d'offre, signature, expédition et facture.
 * Les libellés sont ceux de `apps/portail-fournisseur/src/i18n/fr/portail.json`.
 */
export class Portail {
  constructor(private readonly page: Page) {}

  async attendreStatut(libelle: string) {
    await expect(this.page.getByRole('main').getByText(libelle, { exact: true }).first()).toBeVisible()
  }

  async deposerOffre(appelOffreId: string, o: { montant: number; delai: number; garantie: number }) {
    await allerPortail(this.page, `/appels-offres/${appelOffreId}`)
    await this.page.getByRole('main').getByRole('button', { name: 'Déposer une offre' }).click()
    const dialogue = this.page.getByRole('dialog', { name: 'Déposer une offre' })
    await dialogue.getByRole('textbox', { name: /^Montant proposé/ }).fill(String(o.montant))
    await dialogue.getByRole('textbox', { name: /^Délai de livraison/ }).fill(String(o.delai))
    await dialogue.getByRole('textbox', { name: /^Garantie/ }).fill(String(o.garantie))
    await dialogue.getByRole('textbox', { name: /^Description/ }).fill('Offre déposée sur le portail (E2E)')
    await dialogue.getByRole('button', { name: "Déposer l'offre" }).click()
    await attendreToast(this.page, 'Offre déposée')
    await expect(dialogue).toBeHidden()
  }

  async ouvrirContrat(contratId: string) {
    await allerPortail(this.page, `/contrats/${contratId}`)
  }

  async signerContrat(contratId: string) {
    await this.ouvrirContrat(contratId)
    await this.attendreStatut('À signer')
    await this.page.getByRole('main').getByRole('button', { name: 'Signer le contrat' }).click()
    const confirmation = this.page.getByRole('alertdialog', { name: /^Signer le contrat CTR-/ })
    await confirmation.getByRole('button', { name: 'Signer', exact: true }).click()
    await attendreToast(this.page, 'Contrat signé')
    await this.attendreStatut('Signé')
  }

  async declarerExpedition(contratId: string, commentaire: string) {
    await this.ouvrirContrat(contratId)
    await this.attendreStatut('À expédier')
    await this.page.getByRole('main').getByRole('button', { name: "Déclarer l'expédition" }).click()
    const dialogue = this.page.getByRole('dialog', { name: "Déclarer l'expédition" })
    // Date d'expédition : aujourd'hui par défaut.
    await dialogue.getByRole('textbox', { name: /^Commentaire/ }).fill(commentaire)
    await dialogue.getByRole('button', { name: 'Déclarer', exact: true }).click()
    await attendreToast(this.page, 'Expédition déclarée')
    await expect(dialogue).toBeHidden()
  }

  /** Dépose une facture sur un contrat LIVRE (contrat présélectionné, dates par défaut : J et J+30). */
  async deposerFacture(contratId: string, f: { numero: string; montantHt: number }) {
    // Le dialogue s'ouvre directement (la page en arrière-plan est alors masquée aux technologies d'assistance).
    await this.page.goto(`/factures?nouvelle=${contratId}`)
    const dialogue = this.page.getByRole('dialog', { name: 'Nouvelle facture' })
    await expect(dialogue).toBeVisible()
    await dialogue.getByRole('textbox', { name: /^Numéro de facture/ }).fill(f.numero)
    await dialogue.getByRole('textbox', { name: /^Montant HT/ }).fill(String(f.montantHt))
    await dialogue.getByRole('button', { name: 'Déposer la facture' }).click()
    await attendreToast(this.page, 'Facture déposée')
    await expect(dialogue).toBeHidden()
  }

  ligneFacture(numero: string) {
    return this.page.getByRole('main').getByRole('row').filter({ hasText: numero })
  }

  async ouvrirFactures() {
    await allerPortail(this.page, '/factures')
  }
}
