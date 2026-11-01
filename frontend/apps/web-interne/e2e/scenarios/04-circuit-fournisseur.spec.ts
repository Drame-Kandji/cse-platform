import { AssistantAppelOffre, EcranFinance, FicheAppelOffre, FicheContrat } from '../pages/achats'
import { Portail } from '../pages/portail'
import { fournisseurs } from '../support/env'
import { expect, test } from '../support/fixtures'
import { dateIso, suffixeUnique } from '../support/ui'

/**
 * Scénario 4 : circuit complet avec le fournisseur, en pilotant LES DEUX applications dans le navigateur
 * (plateforme interne http://localhost:5173 et portail fournisseur http://localhost:5174, contrat §13).
 * Le RA publie un AO en invitant F1 (Sahel Engins SA) ; le fournisseur dépose son offre sur le portail ;
 * clôture, évaluation, attribution sous le seuil de délégation (co-validation RM seule, R11) ; signature
 * CSE puis contresignature du fournisseur ; engagement DAF (R06) ; expédition déclarée par le fournisseur ;
 * réception par le RA ; facture déposée sur le portail ; mandatement et paiement par la DAF ; clôture.
 * Aucune donnée partagée avec les autres scénarios : AO, contrat et facture sont créés ici.
 */
test.describe('Scénario 4 · Circuit complet avec le fournisseur', () => {
  test('AO sous le seuil : offre déposée sur le portail, double signature, expédition, facture payée', async ({
    en,
    portail,
  }) => {
    test.setTimeout(420_000)
    const sfx = suffixeUnique()
    const objet = `Acquisition d'un compacteur (E2E portail ${sfx})`
    const montant = 95_000_000 // sous le seuil de délégation de 150 M FCFA : pas d'approbation DG

    // 1. RA : publication de l'AO depuis l'assistant, F1 invité.
    const ra = await en('ra')
    const appelOffreId = await new AssistantAppelOffre(ra).publier({
      objet,
      quantite: 1,
      budget: 110_000_000,
      invite: fournisseurs.sahel.nom,
      dateLimite: dateIso(2),
    })
    const ficheAo = new FicheAppelOffre(ra)
    await ficheAo.attendreStatut('Publié')

    // 2. Fournisseur : dépôt de l'offre sur le portail (invité).
    const fournisseur = await portail()
    const portailF1 = new Portail(fournisseur)
    await portailF1.deposerOffre(appelOffreId, { montant, delai: 20, garantie: 12 })

    // 3. RA : l'offre arrive anonymisée (R10), clôture, note, proposition d'attribution.
    await ficheAo.ouvrir(appelOffreId)
    await expect(ficheAo.panneau('Offres').getByText(/^OFR-/)).toHaveCount(1)
    await expect(ficheAo.panneau('Offres')).not.toContainText(fournisseurs.sahel.nom)
    await ficheAo.cloturer()
    await expect(ficheAo.panneau('Offres')).toContainText(fournisseurs.sahel.nom)
    // Critères par défaut de l'assistant : prix et délai calculés, les quatre autres notés par le RA.
    await ficheAo.noter(fournisseurs.sahel.nom, 80, {
      Garantie: 70,
      'Service après-vente': 75,
      Références: 85,
    })
    await ficheAo.proposerAttribution(fournisseurs.sahel.nom, 'Seule offre conforme, prix dans le budget (E2E)')
    await expect(ra.getByText(/dans le seuil, sans approbation de la Direction Générale/)).toBeVisible()

    // 4. RM : co-validation seule (montant sous le seuil) -> ATTRIBUE + contrat BROUILLON.
    const rm = await en('rm')
    const aoRm = new FicheAppelOffre(rm)
    await aoRm.ouvrir(appelOffreId, 'attribution')
    await aoRm.decider('Co-validation technique', 'Co-valider', 'Conforme (E2E portail)')
    await aoRm.attendreStatut('Attribué')

    // 5. RA : clauses puis signature CSE -> en attente du fournisseur.
    await ficheAo.ouvrir(appelOffreId, 'attribution')
    await ficheAo.attendreStatut('Attribué')
    await ra.getByRole('link', { name: 'Voir le contrat' }).click()
    await expect(ra.getByRole('heading', { name: /^CTR-\d{4}-\d{3}$/, level: 1 })).toBeVisible()
    const numeroContrat = (await ra.getByRole('heading', { level: 1 }).textContent()) ?? ''
    const contratId = /\/contrats\/([0-9a-f-]{36})/.exec(ra.url())?.[1] ?? ''
    const contrat = new FicheContrat(ra)
    await contrat.attendreStatut('Brouillon')
    await contrat.modifierClauses({
      conditionsLivraison: 'Livraison franco sur le chantier (E2E portail)',
      conditionsMaintenance: 'Première révision à la charge du fournisseur',
    })
    await contrat.signer()

    // 6. Fournisseur : contresignature sur le portail -> SIGNE + engagement EN_ATTENTE.
    await portailF1.signerContrat(contratId)
    await contrat.ouvrir(contratId)
    await contrat.attendreStatut('Signé')
    await expect(ra.getByRole('main')).toContainText('Moussa Kane')

    // 7. DAF : validation de l'engagement budgétaire (R06).
    const daf = await en('daf')
    const finance = new EcranFinance(daf)
    await finance.ouvrir()
    await expect(finance.ligneEngagement(numeroContrat)).toContainText('95 000 000')
    await finance.validerEngagement(numeroContrat, 'Crédits disponibles (E2E portail)')

    // 8. Fournisseur : déclaration de l'expédition -> EN_LIVRAISON.
    const commentaire = `Expédié par porte-char (E2E ${sfx})`
    await portailF1.declarerExpedition(contratId, commentaire)

    // 9. RA : l'expédition est visible, confirmation de la livraison -> LIVRE.
    await contrat.ouvrir(contratId)
    await contrat.attendreStatut('En livraison')
    await expect(ra.getByRole('main')).toContainText(commentaire)
    await contrat.confirmerLivraison(`BL-PORTAIL-${sfx}`)

    // 10. Fournisseur : dépôt de la facture sur le contrat livré.
    const numeroFacture = `FA-E2E-${sfx}`
    await portailF1.deposerFacture(contratId, { numero: numeroFacture, montantHt: 80_000_000 })
    await expect(portailF1.ligneFacture(numeroFacture)).toContainText('Reçue')

    // 11. DAF : mandatement puis paiement.
    await finance.ouvrirFactures()
    await expect(finance.ligneFacture(numeroFacture)).toContainText('94 400 000')
    await finance.mandaterPuisPayer(numeroFacture, `MDT-E2E-${sfx}`)

    // 12. Le fournisseur suit le paiement ; le RA clôture le contrat soldé.
    await portailF1.ouvrirFactures()
    await expect(portailF1.ligneFacture(numeroFacture)).toContainText('Payée')
    await contrat.ouvrir(contratId)
    await contrat.cloturer()
  })
})
