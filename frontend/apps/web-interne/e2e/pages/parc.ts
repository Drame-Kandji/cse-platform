import { expect, type Locator, type Page } from '@playwright/test'
import { aller } from '../support/fixtures'
import { attendreToast } from '../support/ui'

/** Fiche d'un matériel (état, actions de cycle de vie, onglets d'historique). */
export class FicheMateriel {
  constructor(private readonly page: Page) {}

  async ouvrir(id: string, designation: string) {
    await aller(this.page, `/materiels/${id}`)
    await expect(this.page.getByRole('heading', { name: designation, level: 1 })).toBeVisible()
  }

  /** Badge d'état affiché sous le titre (« Affecté », « En maintenance », « Disponible »). */
  async attendreEtat(libelle: string) {
    await expect(this.page.getByRole('main').getByText(libelle, { exact: true }).first()).toBeVisible()
  }

  async confirmerReception(observations: string) {
    await this.page.getByRole('button', { name: 'Confirmer la réception' }).click()
    const dialogue = this.page.getByRole('dialog', { name: 'Confirmer la réception' })
    await dialogue.getByRole('textbox', { name: /^Observations à la réception/ }).fill(observations)
    await dialogue.getByRole('button', { name: 'Confirmer la réception' }).click()
    await attendreToast(this.page, 'Réception confirmée')
    await expect(dialogue).toBeHidden()
  }

  async signalerPanne(gravite: 'Mineure' | 'Majeure' | 'Bloquante', description: string) {
    await this.page.getByRole('button', { name: 'Actions', exact: true }).click()
    await this.page.getByRole('menuitem', { name: 'Signaler une panne' }).click()
    const dialogue = this.page.getByRole('dialog', { name: 'Signaler une panne' })
    await dialogue.getByRole('radio', { name: new RegExp(`^${gravite}`) }).check()
    await dialogue.getByRole('textbox', { name: 'Description', exact: true }).fill(description)
    await dialogue.getByRole('button', { name: 'Signaler la panne' }).click()
    await attendreToast(this.page, 'Panne signalée')
    await expect(dialogue).toBeHidden()
  }

  onglet(nom: RegExp) {
    return this.page.getByRole('tab', { name: nom })
  }

  /** Tableau d'historique de l'onglet sélectionné. */
  panneau(): Locator {
    return this.page.getByRole('tabpanel')
  }
}

/** Écran « Maintenance & pannes » (onglets Maintenances / Pannes). */
export class EcranMaintenance {
  constructor(private readonly page: Page) {}

  async ouvrir(onglet: 'Maintenances' | 'Pannes' = 'Maintenances') {
    await aller(this.page, '/maintenances')
    await this.page.getByRole('tab', { name: onglet }).click()
  }

  lignePanne(texte: string) {
    return this.page
      .getByRole('table', { name: 'Pannes signalées' })
      .getByRole('row')
      .filter({ hasText: texte })
  }

  ligneOt(texte: string) {
    return this.page
      .getByRole('table', { name: 'Ordres de travail' })
      .getByRole('row')
      .filter({ hasText: texte })
  }

  async filtrerStatutOt(libelle: string) {
    await this.page.getByRole('combobox', { name: 'Statut' }).click()
    await this.page.getByRole('option', { name: libelle }).click()
  }
}
