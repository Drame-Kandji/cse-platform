package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.in.DeclencherAlertesUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Materiel;

/** Contrôles quotidiens : maintenance préventive due et fins d'affectation proches. */
@Service
@Transactional
public class DeclencherAlertesService implements DeclencherAlertesUseCase {

  private final MaterielRepository materiels;
  private final MaintenanceRepository maintenances;
  private final AffectationRepository affectations;
  private final ChargeurAgregats chargeur;
  private final SuiviEventPublisher evenements;
  private final SuiviParametres parametres;
  private final Clock clock;

  /**
   * @param materiels stockage des matériels
   * @param maintenances stockage des ordres de travail
   * @param affectations stockage des affectations
   * @param chargeur chargement des agrégats
   * @param evenements publication des événements
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public DeclencherAlertesService(
      MaterielRepository materiels,
      MaintenanceRepository maintenances,
      AffectationRepository affectations,
      ChargeurAgregats chargeur,
      SuiviEventPublisher evenements,
      SuiviParametres parametres,
      Clock clock) {
    this.materiels = materiels;
    this.maintenances = maintenances;
    this.affectations = affectations;
    this.chargeur = chargeur;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public int alerterMaintenancesPreventives() {
    LocalDate aujourdhui = LocalDate.now(clock);
    int rappel = parametres.alertes().rappelMaintenanceJours();
    int emises = 0;
    for (Materiel materiel : materiels.findMaintenanceDue(aujourdhui)) {
      if (!materiel.doitEtreAlerte(aujourdhui, rappel)
          || maintenances.existeOuverte(materiel.getId())) {
        continue;
      }
      materiel.marquerAlerteMaintenance(aujourdhui);
      Materiel enregistre = materiels.save(materiel);
      evenements.materiel(
          EventTypes.ALERTE_MAINTENANCE,
          enregistre,
          chargeur.chantierOuNull(enregistre.getChantierActuelId()),
          motif(enregistre));
      emises++;
    }
    return emises;
  }

  @Override
  public int alerterFinsAffectation() {
    LocalDate aujourdhui = LocalDate.now(clock);
    int jours = parametres.alertes().finAffectationJoursAvant();
    int emises = 0;
    for (Affectation affectation :
        affectations.findActivesFinissantAuPlusTard(aujourdhui.plusDays(jours))) {
      if (!affectation.doitAlerterFinProchaine(aujourdhui, jours)) {
        continue;
      }
      affectation.marquerAlerteFinEnvoyee();
      Affectation enregistree = affectations.save(affectation);
      evenements.affectation(
          EventTypes.FIN_AFFECTATION_PROCHAINE,
          enregistree,
          chargeur.materiel(enregistree.getMaterielId()),
          chargeur.chantier(enregistree.getChantierId()));
      emises++;
    }
    return emises;
  }

  private static String motif(Materiel materiel) {
    StringBuilder motif = new StringBuilder("Maintenance préventive due : ");
    motif.append(materiel.heuresDepuisMaintenance()).append(" h depuis la dernière intervention");
    if (materiel.getSeuilMaintenanceHeures() != null) {
      motif.append(" (seuil ").append(materiel.getSeuilMaintenanceHeures()).append(" h)");
    }
    if (materiel.prochaineMaintenance() != null) {
      motif.append(", échéance calendaire au ").append(materiel.prochaineMaintenance());
    }
    return motif.toString();
  }
}
