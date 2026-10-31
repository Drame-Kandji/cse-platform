import { expect, type Page } from '@playwright/test'
import { aller } from '../support/fixtures'
import { attendreToast, choisirDate, choisirOption } from '../support/ui'

export interface SaisieBesoin {
  categorie: 'Matériel roulant' | 'Outillage' | 'Équipement informatique' | 'Consommable'
  designation: string
  quantite: number
  duree: number
  /** Libellé de l'option du chantier (« CH-DKR-01 · … »). */
  chantier: RegExp
  dateBesoin: string
  priorite: 'Normale' | 'Urgente' | 'Critique'
  justification?: string
}

/** Formulaire « Nouveau besoin » (CHEF / RM). */
export class FormulaireBesoin {
  constructor(private readonly page: Page) {}

  async ouvrir() {
    await aller(this.page, '/besoins/nouveau')
    await expect(this.page.getByRole('heading', { name: 'Nouveau besoin', level: 1 })).toBeVisible()
  }

  async remplir(b: SaisieBesoin) {
    const p = this.page
    await p.getByRole('radio', { name: b.categorie, exact: true }).check()
    await p.getByRole('textbox', { name: 'Désignation' }).fill(b.designation)
    await p.getByRole('spinbutton', { name: 'Quantité' }).fill(String(b.quantite))
    await p.getByRole('spinbutton', { name: "Durée d'utilisation" }).fill(String(b.duree))
    await choisirOption(p, p.getByRole('combobox', { name: 'Chantier' }), b.chantier)
    await choisirDate(p, p.getByRole('button', { name: 'Date souhaitée' }), b.dateBesoin)
    await p.getByRole('radio', { name: new RegExp(`^${b.priorite} `) }).check()
    if (b.justification !== undefined) {
      await this.justification().fill(b.justification)
    }
  }

  justification() {
    return this.page.getByRole('textbox', { name: /^Justification/ })
  }

  async envoyer() {
    await this.page.getByRole('button', { name: 'Envoyer le besoin' }).click()
  }

  /** Envoie et attend la redirection vers la fiche : renvoie l'identifiant du besoin créé. */
  async envoyerEtAttendreFiche(designation: string): Promise<string> {
    await this.envoyer()
    await attendreToast(this.page, 'Besoin enregistré')
    await expect(this.page.getByRole('heading', { name: designation, level: 1 })).toBeVisible()
    const id = /\/besoins\/([0-9a-f-]{36})$/.exec(this.page.url())?.[1]
    if (!id) throw new Error(`URL de fiche inattendue : ${this.page.url()}`)
    return id
  }
}

/** Fiche d'un besoin : statut, avancement, matériels affectés, historique. */
export class FicheBesoin {
  constructor(private readonly page: Page) {}

  async ouvrir(id: string) {
    await aller(this.page, `/besoins/${id}`)
  }

  /** Badge de statut affiché sous le titre. */
  async attendreStatut(libelle: string) {
    await expect(this.page.getByRole('main').getByText(libelle, { exact: true }).first()).toBeVisible()
  }

  reference() {
    return this.page
      .getByRole('main')
      .getByText(/^BES-\d{4}-\d{4}$/)
      .first()
  }

  historique() {
    return this.page.getByRole('list', { name: 'Historique' })
  }

  /** Lien vers un matériel affecté (section « Matériels affectés »). */
  materielAffecte(code: string) {
    return this.page.getByRole('main').getByRole('link', { name: new RegExp(code) })
  }
}

/** Écran « Validation des besoins » du Responsable Matériel. */
export class FileValidation {
  constructor(private readonly page: Page) {}

  async ouvrir() {
    await aller(this.page, '/validation')
    await expect(this.page.getByRole('heading', { name: 'Validation des besoins' })).toBeVisible()
  }

  /** Carte d'un besoin de la file, repérée par sa désignation unique. */
  carte(designation: string) {
    return this.page
      .getByRole('list', { name: "File d'attente" })
      .getByRole('listitem')
      .filter({ hasText: designation })
  }

  async valider(designation: string, commentaire: string) {
    await this.carte(designation).getByRole('button', { name: 'Valider', exact: true }).click()
    const dialogue = this.page.getByRole('dialog', { name: /^Valider le besoin BES-/ })
    await expect(dialogue).toBeVisible()
    return {
      dialogue,
      confirmer: async () => {
        await dialogue.getByRole('textbox', { name: /^Commentaire/ }).fill(commentaire)
        await dialogue.getByRole('button', { name: 'Valider le besoin' }).click()
        await attendreToast(this.page, 'Besoin validé')
        await expect(dialogue).toBeHidden()
      },
    }
  }

  async ouvrirRejet(designation: string) {
    await this.carte(designation).getByRole('button', { name: 'Rejeter', exact: true }).click()
    const dialogue = this.page.getByRole('dialog', { name: /^Rejeter le besoin BES-/ })
    await expect(dialogue).toBeVisible()
    return dialogue
  }
}

/** Centre de notifications de l'utilisateur courant (rechargé jusqu'à l'arrivée, événement Kafka). */
export async function attendreNotification(page: Page, titre: string | RegExp) {
  await expect(async () => {
    await aller(page, '/notifications')
    await expect(page.getByRole('main').getByRole('button', { name: titre }).first()).toBeVisible({
      timeout: 2_000,
    })
  }).toPass({ timeout: 60_000, intervals: [1_000, 2_000, 3_000] })
}
