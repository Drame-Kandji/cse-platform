import { expect, type Page } from '@playwright/test'
import { aller } from '../support/fixtures'
import { attendreToast, choisirDate, choisirOption, dateIso } from '../support/ui'

/** Fiche d'un appel d'offres : offres, évaluation, attribution et circuit d'approbation (R11). */
export class FicheAppelOffre {
  constructor(private readonly page: Page) {}

  async ouvrir(id: string, onglet?: 'offres' | 'evaluation' | 'attribution') {
    await aller(this.page, `/appels-offres/${id}${onglet ? `?onglet=${onglet}` : ''}`)
  }

  async attendreStatut(libelle: 'Publié' | 'Clôturé' | 'Attribué') {
    await expect(this.page.getByRole('main').getByText(libelle, { exact: true }).first()).toBeVisible()
  }

  panneau(nom: 'Offres' | 'Évaluation' | 'Attribution') {
    return this.page.getByRole('tabpanel', { name: nom })
  }

  async saisirOffre(o: { fournisseur: string; montant: number; delai: number; garantie: number }) {
    await this.page.getByRole('button', { name: 'Saisir une offre reçue' }).click()
    const dialogue = this.page.getByRole('dialog', { name: 'Saisir une offre reçue' })
    await choisirOption(
      this.page,
      dialogue.getByRole('combobox', { name: 'Fournisseur' }),
      `${o.fournisseur} (invité)`,
    )
    await dialogue.getByRole('spinbutton', { name: 'Montant (FCFA)' }).fill(String(o.montant))
    await dialogue.getByRole('spinbutton', { name: 'Délai (jours)' }).fill(String(o.delai))
    await dialogue.getByRole('spinbutton', { name: /^Garantie \(mois\)/ }).fill(String(o.garantie))
    await dialogue.getByRole('button', { name: "Enregistrer l'offre" }).click()
    await attendreToast(this.page, 'Offre enregistrée')
    await expect(dialogue).toBeHidden()
  }

  async cloturer() {
    await this.page.getByRole('main').getByRole('button', { name: 'Clôturer', exact: true }).click()
    const confirmation = this.page.getByRole('alertdialog', { name: "Clôturer l'appel d'offres ?" })
    await confirmation.getByRole('button', { name: 'Clôturer', exact: true }).click()
    await attendreToast(this.page, /Appel d'offres clôturé/)
    await this.attendreStatut('Clôturé')
  }

  async noter(fournisseur: string, qualiteTechnique: number) {
    await this.page.getByRole('tab', { name: 'Évaluation' }).click()
    await this.panneau('Évaluation')
      .getByRole('button', { name: new RegExp(`^Noter ${fournisseur}`) })
      .click()
    const dialogue = this.page.getByRole('dialog', { name: new RegExp(`^Évaluer l'offre ${fournisseur}`) })
    await dialogue.getByRole('spinbutton', { name: 'Qualité technique' }).fill(String(qualiteTechnique))
    await dialogue.getByRole('button', { name: 'Enregistrer les notes' }).click()
    await attendreToast(this.page, 'Évaluation enregistrée')
    await expect(dialogue).toBeHidden()
  }

  async proposerAttribution(fournisseur: string, justification: string) {
    await this.page.getByRole('tab', { name: 'Attribution' }).click()
    const panneau = this.panneau('Attribution')
    await panneau.getByRole('radio', { name: new RegExp(`^1\\. ${fournisseur} .*recommandée$`) }).check()
    await panneau.getByRole('textbox', { name: 'Justification' }).fill(justification)
    await panneau.getByRole('button', { name: "Proposer l'attribution" }).click()
    await attendreToast(this.page, 'Attribution proposée : en attente des validations')
  }

  /** Étape du circuit d'approbation (« Co-validation technique », « Approbation de la Direction Générale »). */
  etape(titre: string) {
    return this.page
      .getByRole('list', { name: "Circuit d'approbation" })
      .getByRole('listitem')
      .filter({ hasText: titre })
  }

  async decider(titreEtape: string, bouton: 'Co-valider' | 'Approuver', commentaire: string) {
    await this.page.getByRole('tab', { name: 'Attribution' }).click()
    await this.etape(titreEtape).getByRole('button', { name: bouton }).click()
    const dialogue = this.page.getByRole('dialog', { name: /^Approuver · / })
    await dialogue.getByRole('textbox', { name: /^Commentaire/ }).fill(commentaire)
    await dialogue.getByRole('button', { name: 'Confirmer l’approbation' }).click()
    await attendreToast(this.page, 'Décision enregistrée')
    await expect(dialogue).toBeHidden()
    await expect(this.etape(titreEtape)).toContainText('Approuvée')
  }
}

/** Fiche d'un contrat : clauses, signature, livraison. */
export class FicheContrat {
  constructor(private readonly page: Page) {}

  async ouvrir(id: string) {
    await aller(this.page, `/contrats/${id}`)
  }

  async attendreStatut(libelle: string) {
    await expect(this.page.getByRole('main').getByText(libelle, { exact: true }).first()).toBeVisible()
  }

  async modifierClauses(c: { conditionsLivraison: string; conditionsMaintenance: string }) {
    await this.page.getByRole('button', { name: 'Modifier les clauses' }).click()
    const dialogue = this.page.getByRole('dialog', { name: 'Modifier les clauses' })
    await dialogue.getByRole('textbox', { name: /^Conditions de livraison/ }).fill(c.conditionsLivraison)
    await dialogue.getByRole('textbox', { name: /^Conditions de maintenance/ }).fill(c.conditionsMaintenance)
    await dialogue.getByRole('button', { name: 'Enregistrer' }).click()
    await attendreToast(this.page, 'Clauses enregistrées')
    await expect(dialogue).toBeHidden()
  }

  async signer() {
    await this.page.getByRole('button', { name: 'Signer le contrat' }).click()
    const confirmation = this.page.getByRole('alertdialog', { name: /^Signer le contrat CTR-/ })
    await confirmation.getByRole('button', { name: 'Signer le contrat' }).click()
    await attendreToast(this.page, 'Contrat signé : engagement transmis au DAF')
    await this.attendreStatut('Signé')
  }

  async demarrerLivraison() {
    await this.page.getByRole('button', { name: 'Démarrer la livraison' }).click()
    const confirmation = this.page.getByRole('alertdialog', { name: 'Démarrer la livraison ?' })
    await confirmation.getByRole('button', { name: 'Démarrer la livraison' }).click()
    await attendreToast(this.page, 'Livraison démarrée')
    await this.attendreStatut('En livraison')
  }

  async confirmerLivraison(bonLivraison: string) {
    await this.page.getByRole('button', { name: 'Confirmer la livraison' }).click()
    const dialogue = this.page.getByRole('dialog', { name: 'Confirmer la livraison' })
    await choisirDate(this.page, dialogue.getByRole('button', { name: 'Livraison effective' }), dateIso(0))
    await dialogue.getByRole('textbox', { name: 'Bon de livraison' }).fill(bonLivraison)
    await dialogue
      .getByRole('textbox', { name: /^Observations/ })
      .fill('Réception provisoire sans réserve (E2E)')
    await dialogue.getByRole('button', { name: 'Confirmer la livraison' }).click()
    await attendreToast(this.page, 'Livraison confirmée')
    await this.attendreStatut('Livré')
  }
}

/** Écran Finance du DAF : engagements budgétaires à valider (R06). */
export class EcranFinance {
  constructor(private readonly page: Page) {}

  async ouvrir() {
    await aller(this.page, '/finance')
  }

  ligneEngagement(numeroContrat: string) {
    return this.page
      .getByRole('table', { name: 'Engagements budgétaires' })
      .getByRole('row')
      .filter({ hasText: numeroContrat })
  }

  async validerEngagement(numeroContrat: string, commentaire: string) {
    await this.ligneEngagement(numeroContrat).getByRole('button', { name: 'Valider' }).click()
    const dialogue = this.page.getByRole('dialog', { name: `Valider l'engagement ${numeroContrat}` })
    await dialogue.getByRole('textbox', { name: /^Commentaire/ }).fill(commentaire)
    await dialogue.getByRole('button', { name: 'Valider', exact: true }).click()
    await attendreToast(this.page, 'Engagement validé')
    await expect(dialogue).toBeHidden()
  }
}
