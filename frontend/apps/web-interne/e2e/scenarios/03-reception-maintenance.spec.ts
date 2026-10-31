import { attendreNotification } from '../pages/besoins'
import { EcranMaintenance, FicheMateriel } from '../pages/parc'
import { api, type AffectationDto, type MaterielDto } from '../support/api'
import { chantiers } from '../support/env'
import { expect, test } from '../support/fixtures'
import { attendreToast, dateIso, suffixeUnique } from '../support/ui'

/**
 * Scénario critique 3 (spec §6.2) : le chef confirme la réception d'un matériel affecté à son chantier,
 * signale une panne vue par le RM, qui planifie, démarre puis clôture la maintenance (coût saisi) ;
 * le matériel revient à l'état AFFECTE (affectation en cours).
 */
test.describe('Scénario 3 · Réception et clôture de maintenance', () => {
  test('réception confirmée par le chef, panne signalée, OT correctif planifié, démarré et clôturé par le RM', async ({
    en,
  }) => {
    const sfx = suffixeUnique()
    const designation = `Compacteur E2E ${sfx}`
    // Préparation : matériel neuf affecté au chantier du chef (affectation EN_ATTENTE_RECEPTION).
    const materiel = await api<MaterielDto>('rm', 'POST', '/api/materiels', {
      code: `E2E-CMP-${sfx}`,
      designation,
      categorie: 'MATERIEL_ROULANT',
      tarifJournalier: 90000,
    })
    const affectation = await api<AffectationDto>('rm', 'POST', `/api/materiels/${materiel.id}/affecter`, {
      chantierId: chantiers.dakar.id,
      dateDebut: dateIso(0),
      dateFinPrevue: dateIso(20),
    })
    expect(affectation.statut).toBe('EN_ATTENTE_RECEPTION')

    // 1. Le chef confirme la réception sur la fiche du matériel.
    const chef = await en('chef')
    const ficheChef = new FicheMateriel(chef)
    await ficheChef.ouvrir(materiel.id, designation)
    await ficheChef.attendreEtat('Affecté')
    await expect(ficheChef.panneau()).toContainText('En attente de réception')
    await ficheChef.confirmerReception('Reçu en bon état, carnet d’entretien remis (E2E)')
    await expect(ficheChef.panneau()).toContainText('En cours')
    await expect(chef.getByRole('button', { name: 'Confirmer la réception' })).toBeHidden()

    // 2. Le chef signale une panne majeure.
    const descriptionPanne = `Fuite hydraulique sur le vérin avant (E2E ${sfx})`
    await ficheChef.signalerPanne('Majeure', descriptionPanne)

    // 3. Le RM est alerté et voit la panne dans l'onglet Pannes.
    const rm = await en('rm')
    await attendreNotification(rm, new RegExp(`Panne .* : ${designation}`))
    const ecran = new EcranMaintenance(rm)
    await ecran.ouvrir('Pannes')
    const panne = ecran.lignePanne(descriptionPanne)
    await expect(panne).toContainText('Majeure')
    await expect(panne).toContainText('Signalée')
    await expect(panne).toContainText(materiel.code)

    // 4. Le RM crée l'OT correctif depuis la panne (matériel pré-rempli).
    await panne.getByRole('button', { name: 'Créer un OT' }).click()
    const planifier = rm.getByRole('dialog', { name: 'Créer un ordre de travail' })
    await expect(planifier.getByRole('combobox', { name: 'Matériel' })).toContainText(materiel.code)
    await planifier.getByRole('radio', { name: 'Corrective' }).check()
    await planifier.getByRole('textbox', { name: /^Technicien/ }).fill('Atelier central E2E')
    await planifier
      .getByRole('textbox', { name: 'Travaux à réaliser' })
      .fill('Remplacement du joint du vérin avant')
    await planifier.getByRole('button', { name: 'Planifier' }).click()
    await attendreToast(rm, 'Maintenance planifiée')
    await expect(panne.getByText('OT lié')).toBeVisible()

    // 5. Démarrage de l'OT : le matériel passe EN_MAINTENANCE.
    await ecran.ouvrir('Maintenances')
    await ecran.filtrerStatutOt('Planifiée')
    const ot = ecran.ligneOt(materiel.code)
    await expect(ot).toContainText('Corrective')
    await ot.getByRole('button', { name: 'Démarrer' }).click()
    const demarrer = rm.getByRole('alertdialog', { name: /^Démarrer l'OT OT-/ })
    await demarrer.getByRole('button', { name: 'Démarrer' }).click()
    await attendreToast(rm, 'Maintenance démarrée')
    const ficheRm = new FicheMateriel(rm)
    await ficheRm.ouvrir(materiel.id, designation)
    await ficheRm.attendreEtat('En maintenance')

    // 6. Clôture avec coût : retour à l'état AFFECTE (affectation toujours en cours).
    await ecran.ouvrir('Maintenances')
    await ecran.filtrerStatutOt('En cours')
    const otEnCours = ecran.ligneOt(materiel.code)
    await otEnCours.getByRole('button', { name: 'Clôturer' }).click()
    const cloturer = rm.getByRole('dialog', { name: /^Clôturer l'OT OT-/ })
    await cloturer.getByRole('spinbutton', { name: 'Coût total (FCFA)' }).fill('350000')
    await cloturer.getByRole('textbox', { name: /^Observations/ }).fill('Joint remplacé, essais concluants')
    await cloturer.getByRole('button', { name: "Clôturer l'OT" }).click()
    await attendreToast(rm, 'Maintenance clôturée')

    await ficheRm.ouvrir(materiel.id, designation)
    await ficheRm.attendreEtat('Affecté')
    await ficheRm.onglet(/^Maintenances/).click()
    await expect(ficheRm.panneau()).toContainText('Clôturée')
    await expect(ficheRm.panneau()).toContainText('350 000')
    // La panne liée à l'OT est résolue par la clôture.
    await ecran.ouvrir('Pannes')
    await expect(ecran.lignePanne(descriptionPanne)).toContainText('Résolue')
    const final = await api<MaterielDto>('rm', 'GET', `/api/materiels/${materiel.id}`)
    expect(final.etat).toBe('AFFECTE')
    expect(final.chantierActuelId).toBe(chantiers.dakar.id)
  })
})
