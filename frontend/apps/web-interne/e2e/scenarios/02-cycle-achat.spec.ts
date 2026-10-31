import { EcranFinance, FicheAppelOffre, FicheContrat } from '../pages/achats'
import { attendreNotification } from '../pages/besoins'
import { FicheMateriel } from '../pages/parc'
import { type AffectationDto, api, attendre, type BesoinDto, type Page as PageApi } from '../support/api'
import { chantiers, fournisseurs } from '../support/env'
import { expect, test } from '../support/fixtures'
import { dateIso, suffixeUnique } from '../support/ui'

interface AppelOffreDto {
  id: string
  reference: string
  statut: string
  contratId: string | null
}

/**
 * Scénario critique 2 (spec §6.2) : cycle complet d'achat, du besoin orienté vers les achats jusqu'à la
 * livraison et à la création des matériels dans ms-suivi. Les étapes préparatoires (besoin, publication
 * de l'AO) passent par l'API ; les décisions (offres, clôture, notes, attribution, co-validation RM,
 * approbation DG au-delà du seuil R11, signature, engagement DAF R06, livraison) passent par l'interface.
 */
test.describe('Scénario 2 · Cycle complet d’achat', () => {
  test('AO au-delà du seuil de délégation : offres, attribution co-validée par le RM et approuvée par la DG, engagement DAF, livraison', async ({
    en,
  }) => {
    test.setTimeout(300_000)
    const sfx = suffixeUnique()
    const designation = `Niveleuse E2E ${sfx}`

    // Préparation (API) : besoin du chef, validé par le RM, sans matériel disponible -> EN_ACHAT.
    const besoin = await api<BesoinDto>('chef', 'POST', '/api/besoins', {
      chantierId: chantiers.dakar.id,
      designation,
      categorie: 'MATERIEL_ROULANT',
      quantite: 1,
      priorite: 'NORMAL',
      dureeUtilisationJours: 60,
      dateBesoin: dateIso(5),
    })
    const valide = await api<BesoinDto>('rm', 'PATCH', `/api/besoins/${besoin.id}/valider`, {
      commentaire: 'Aucune niveleuse disponible : achat (E2E)',
    })
    expect(valide.statut).toBe('EN_ACHAT')
    await attendre(
      () => api<PageApi<{ besoinId: string }>>('ra', 'GET', '/api/besoins-a-acheter?size=50'),
      (p) => p.content.some((b) => b.besoinId === besoin.id),
      'projection BesoinAAcheter dans ms-achats',
    )
    const ao = await api<AppelOffreDto>('ra', 'POST', '/api/appels-offres', {
      besoinId: besoin.id,
      objet: `Acquisition d'une niveleuse (E2E ${sfx})`,
      type: 'APPEL_OFFRES',
      categorie: 'MATERIEL_ROULANT',
      quantite: 1,
      budgetEstime: 200_000_000,
      dateLimite: new Date(Date.now() + 30 * 60_000).toISOString(),
      criteres: [
        { code: 'PRIX', poids: 50 },
        { code: 'DELAI', poids: 20 },
        { code: 'QUALITE_TECHNIQUE', poids: 30 },
      ],
      fournisseurIdsInvites: [fournisseurs.sahel.id, fournisseurs.teranga.id],
    })

    // 1. RA : saisie de deux offres reçues hors portail, anonymisées tant que l'AO est publié (R10).
    const ra = await en('ra')
    const ficheAo = new FicheAppelOffre(ra)
    await ficheAo.ouvrir(ao.id)
    await ficheAo.attendreStatut('Publié')
    await ficheAo.saisirOffre({
      fournisseur: fournisseurs.sahel.nom,
      montant: 160_000_000,
      delai: 20,
      garantie: 24,
    })
    await ficheAo.saisirOffre({
      fournisseur: fournisseurs.teranga.nom,
      montant: 175_000_000,
      delai: 25,
      garantie: 12,
    })
    await expect(ficheAo.panneau('Offres')).not.toContainText(fournisseurs.sahel.nom)
    await expect(ficheAo.panneau('Offres').getByText(/^OFR-/)).toHaveCount(2)

    // 2. Clôture anticipée (lève l'anonymat), notes de qualité technique, proposition d'attribution.
    await ficheAo.cloturer()
    await expect(ficheAo.panneau('Offres')).toContainText(fournisseurs.sahel.nom)
    await ficheAo.noter(fournisseurs.sahel.nom, 85)
    await ficheAo.noter(fournisseurs.teranga.nom, 75)
    await ficheAo.proposerAttribution(fournisseurs.sahel.nom, 'Meilleur rapport prix / délai (E2E)')
    await expect(ra.getByText(/supérieur au seuil de délégation/)).toBeVisible()

    // 3. RM : co-validation technique (R11).
    const rm = await en('rm')
    const aoRm = new FicheAppelOffre(rm)
    await aoRm.ouvrir(ao.id, 'attribution')
    await aoRm.decider('Co-validation technique', 'Co-valider', 'Conforme au besoin du chantier (E2E)')
    await aoRm.attendreStatut('Clôturé')

    // 4. DG : approbation au-delà du seuil de délégation -> ATTRIBUE + contrat BROUILLON (R03).
    const dg = await en('dg')
    await attendreNotification(dg, new RegExp(`Attribution ${ao.reference} à approuver`))
    const aoDg = new FicheAppelOffre(dg)
    await aoDg.ouvrir(ao.id, 'attribution')
    await aoDg.decider('Approbation de la Direction Générale', 'Approuver', 'Approuvé (E2E)')
    await aoDg.attendreStatut('Attribué')

    // 5. RA : clauses puis signature du contrat généré.
    await ficheAo.ouvrir(ao.id, 'attribution')
    await ficheAo.attendreStatut('Attribué')
    await ra.getByRole('link', { name: 'Voir le contrat' }).click()
    await expect(ra.getByRole('heading', { name: /^CTR-\d{4}-\d{3}$/, level: 1 })).toBeVisible()
    const numeroContrat = (await ra.getByRole('heading', { level: 1 }).textContent()) ?? ''
    const contratId = /\/contrats\/([0-9a-f-]{36})/.exec(ra.url())?.[1] ?? ''
    const contrat = new FicheContrat(ra)
    await contrat.attendreStatut('Brouillon')
    await contrat.modifierClauses({
      conditionsLivraison: 'Livraison franco sur le chantier Dakar-AIBD',
      conditionsMaintenance: 'Révision des 500 heures par le fournisseur',
    })
    await expect(ra.getByRole('main')).toContainText('Livraison franco sur le chantier Dakar-AIBD')
    await contrat.signer()
    // R06 : la livraison ne peut démarrer qu'après l'engagement DAF.
    await expect(ra.getByRole('button', { name: 'Démarrer la livraison' })).toBeDisabled()

    // 6. DAF : validation de l'engagement budgétaire.
    const daf = await en('daf')
    const finance = new EcranFinance(daf)
    await finance.ouvrir()
    await expect(finance.ligneEngagement(numeroContrat)).toContainText('160 000 000')
    await finance.validerEngagement(numeroContrat, 'Crédits disponibles (E2E)')
    await expect(finance.ligneEngagement(numeroContrat)).toHaveCount(0)

    // 7. RA : démarrage puis confirmation de la livraison.
    await contrat.ouvrir(contratId)
    await contrat.attendreStatut('Engagement validé')
    await contrat.demarrerLivraison()
    await contrat.confirmerLivraison(`BL-E2E-${sfx}`)
    await expect(ra.getByRole('main')).toContainText(`BL-E2E-${sfx}`)

    // 8. ms-suivi (cohérence à terme, LIVRAISON_EFFECTUEE) : matériel créé et affecté au chantier du besoin.
    const affectations = await attendre(
      () =>
        api<PageApi<AffectationDto>>(
          'rm',
          'GET',
          `/api/affectations?chantierId=${chantiers.dakar.id}&statut=EN_ATTENTE_RECEPTION&size=50`,
        ),
      (p) => p.content.some((a) => a.contratId === contratId),
      'affectation EN_ATTENTE_RECEPTION créée par ms-suivi',
    )
    const affectation = affectations.content.find((a) => a.contratId === contratId)
    expect(affectation?.besoinId).toBe(besoin.id)
    expect(affectation?.materielDesignation).toBe(designation)

    // Le chef voit le nouveau matériel sur son chantier, réception à confirmer.
    const chef = await en('chef')
    const ficheMateriel = new FicheMateriel(chef)
    await ficheMateriel.ouvrir(affectation?.materielId ?? '', designation)
    await expect(ficheMateriel.panneau()).toContainText('En attente de réception')
    await expect(chef.getByRole('button', { name: 'Confirmer la réception' })).toBeVisible()
  })
})
