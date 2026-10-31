import { attendreNotification, FicheBesoin, FileValidation, FormulaireBesoin } from '../pages/besoins'
import { api, type BesoinDto, type MaterielDto } from '../support/api'
import { chantiers } from '../support/env'
import { expect, test } from '../support/fixtures'
import { attendreToast, dateIso, suffixeUnique } from '../support/ui'

/**
 * Scénario critique 1 (spec §6.2) : soumission d'un besoin par le chef de chantier, arbitrage par le
 * Responsable Matériel (R01) dans l'écran Validation, historique et notification du demandeur.
 */
test.describe('Scénario 1 · Soumission et validation d’un besoin', () => {
  test('besoin URGENT soumis par le chef, validé par le RM et affecté directement depuis le parc', async ({
    en,
  }) => {
    const sfx = suffixeUnique()
    const designation = `Pelle E2E ${sfx}`
    // Préparation : un matériel DISPONIBLE dont la désignation est unique garantit l'affectation directe.
    const materiel = await api<MaterielDto>('rm', 'POST', '/api/materiels', {
      code: `E2E-PEL-${sfx}`,
      designation,
      categorie: 'MATERIEL_ROULANT',
      tarifJournalier: 150000,
    })

    // 1. Le chef exprime le besoin ; R08 : la justification est exigée pour une priorité urgente.
    const chef = await en('chef')
    const formulaire = new FormulaireBesoin(chef)
    await formulaire.ouvrir()
    await formulaire.remplir({
      categorie: 'Matériel roulant',
      designation,
      quantite: 1,
      duree: 10,
      chantier: /^CH-DKR-01 · /,
      dateBesoin: dateIso(1),
      priorite: 'Urgente',
    })
    await formulaire.envoyer()
    await expect(
      chef.getByText('Justification obligatoire pour une priorité urgente ou critique'),
    ).toBeVisible()
    await expect(formulaire.justification()).toHaveAttribute('aria-invalid', 'true')
    await formulaire.justification().fill('Pelle principale en panne, terrassement à l’arrêt (E2E).')
    const besoinId = await formulaire.envoyerEtAttendreFiche(designation)
    const ficheChef = new FicheBesoin(chef)
    await ficheChef.attendreStatut('En attente')
    const reference = (await ficheChef.reference().textContent()) ?? ''
    expect(reference).toMatch(/^BES-\d{4}-\d{4}$/)

    // 2. Le RM retrouve le besoin dans sa file, constate la disponibilité et valide.
    const rm = await en('rm')
    const file = new FileValidation(rm)
    await file.ouvrir()
    await expect(file.carte(designation)).toContainText('Urgente')
    await expect(file.carte(designation)).toContainText(chantiers.dakar.nom)
    const validation = await file.valider(designation, 'Validé en E2E : pelle disponible au parc')
    await expect(validation.dialogue.getByText(/1 disponible\(s\) pour 1 demandé\(s\)/)).toBeVisible()
    await validation.dialogue.getByRole('checkbox', { name: new RegExp(materiel.code) }).check()
    await validation.confirmer()
    await expect(file.carte(designation)).toHaveCount(0)

    // 3. Fiche : statut final AFFECTE_DIRECT, matériel lié, historique complet.
    const ficheRm = new FicheBesoin(rm)
    await ficheRm.ouvrir(besoinId)
    await ficheRm.attendreStatut('Affecté directement')
    await expect(ficheRm.materielAffecte(materiel.code)).toBeVisible()
    const historique = ficheRm.historique()
    // Du plus récent au plus ancien : orientation, affectation du matériel, validation, soumission.
    // L'entrée « Matériel affecté » arrive par événement Kafka (ms-suivi) : rechargement si besoin.
    await expect(async () => {
      await ficheRm.ouvrir(besoinId)
      await expect(historique.getByRole('listitem')).toHaveCount(4, { timeout: 2_000 })
    }).toPass({ timeout: 30_000 })
    await expect(historique.getByRole('listitem').first()).toContainText('Affectation directe')
    await expect(historique).toContainText(`Matériel ${materiel.code}`)
    await expect(historique).toContainText('Validé en E2E : pelle disponible au parc')
    await expect(historique.getByRole('listitem').last()).toContainText('Soumission')

    // 4. Le chef est notifié (événement BESOIN_AFFECTE_DIRECT via Kafka) et voit le nouveau statut.
    await attendreNotification(chef, new RegExp(`Besoin ${reference} satisfait par le parc`))
    await ficheChef.ouvrir(besoinId)
    await ficheChef.attendreStatut('Affecté directement')
    const besoin = await api<BesoinDto>('chef', 'GET', `/api/besoins/${besoinId}`)
    expect(besoin.statut).toBe('AFFECTE_DIRECT')
  })

  test('rejet d’un besoin par le RM : motif obligatoire, statut REJETE et notification du chef', async ({
    en,
  }) => {
    const designation = `Bétonnière E2E ${suffixeUnique()}`
    // Préparation : besoin NORMAL soumis par le chef (la soumission par l'interface est couverte ci-dessus).
    const besoin = await api<BesoinDto>('chef', 'POST', '/api/besoins', {
      chantierId: chantiers.thies.id,
      designation,
      categorie: 'OUTILLAGE',
      quantite: 1,
      priorite: 'NORMAL',
      dureeUtilisationJours: 5,
      dateBesoin: dateIso(3),
    })

    const rm = await en('rm')
    const file = new FileValidation(rm)
    await file.ouvrir()
    const dialogue = await file.ouvrirRejet(designation)
    // Motif vide : refus côté formulaire (R08), le besoin reste en attente.
    await dialogue.getByRole('button', { name: 'Rejeter le besoin' }).click()
    await expect(dialogue.getByText('Motif obligatoire (5 caractères au minimum)')).toBeVisible()
    const motif = 'Doublon avec une bétonnière déjà présente sur le chantier (E2E)'
    await dialogue.getByRole('textbox', { name: 'Motif du rejet' }).fill(motif)
    await dialogue.getByRole('button', { name: 'Rejeter le besoin' }).click()
    await attendreToast(rm, 'Besoin rejeté')
    await expect(file.carte(designation)).toHaveCount(0)

    const fiche = new FicheBesoin(rm)
    await fiche.ouvrir(besoin.id)
    await fiche.attendreStatut('Rejeté')
    await expect(rm.getByRole('main')).toContainText(motif)
    await expect(fiche.historique()).toContainText('Rejet')

    const chef = await en('chef')
    await attendreNotification(chef, new RegExp(`Besoin ${besoin.reference} rejeté`))
  })
})
