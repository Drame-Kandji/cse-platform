package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.CloturerMaintenanceCommande;
import sn.cse.suivi.application.command.PlanifierMaintenanceCommande;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.in.GererMaintenancesUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.GenerateurSequence;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.MaintenanceVue;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.StatutPanne;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Planification, démarrage et clôture des ordres de travail. */
@Service
@Transactional
public class GererMaintenancesService implements GererMaintenancesUseCase {

  /** Série de numérotation des ordres de travail. */
  static final String SERIE_OT = "ot";

  private final MaintenanceRepository maintenances;
  private final MaterielRepository materiels;
  private final AffectationRepository affectations;
  private final PanneRepository pannes;
  private final MouvementRepository mouvements;
  private final GenerateurSequence sequences;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;
  private final SuiviEventPublisher evenements;
  private final SuiviParametres parametres;
  private final Clock clock;

  /**
   * @param maintenances stockage des ordres de travail
   * @param materiels stockage des matériels
   * @param affectations stockage des affectations
   * @param pannes stockage des pannes
   * @param mouvements journal des mouvements
   * @param sequences numérotation
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   * @param evenements publication des événements
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public GererMaintenancesService(
      MaintenanceRepository maintenances,
      MaterielRepository materiels,
      AffectationRepository affectations,
      PanneRepository pannes,
      MouvementRepository mouvements,
      GenerateurSequence sequences,
      ChargeurAgregats chargeur,
      AssembleurVues vues,
      SuiviEventPublisher evenements,
      SuiviParametres parametres,
      Clock clock) {
    this.maintenances = maintenances;
    this.materiels = materiels;
    this.affectations = affectations;
    this.pannes = pannes;
    this.mouvements = mouvements;
    this.sequences = sequences;
    this.chargeur = chargeur;
    this.vues = vues;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public MaintenanceVue planifier(PlanifierMaintenanceCommande commande) {
    Materiel materiel = chargeur.materiel(commande.materielId());
    UUID panneId = commande.planification().panneId();
    Panne panne = panneId == null ? null : chargeur.panne(panneId);
    Maintenance maintenance = Maintenance.planifier(numeroOt(), materiel, commande.planification());
    if (panne != null) {
      panne.prendreEnCharge(maintenance.getId(), materiel.getId());
    }
    Maintenance enregistree = maintenances.save(maintenance);
    if (panne != null) {
      pannes.save(panne);
    }
    evenements.maintenance(EventTypes.MAINTENANCE_PLANIFIEE, enregistree, materiel);
    return vues.maintenance(enregistree);
  }

  @Override
  public MaintenanceVue demarrer(UUID maintenanceId, CurrentUser auteur) {
    Maintenance maintenance = chargeur.maintenance(maintenanceId);
    Materiel materiel = chargeur.materiel(maintenance.getMaterielId());
    maintenance.demarrer(LocalDate.now(clock));
    materiel.mettreEnMaintenance();
    Maintenance enregistree = maintenances.save(maintenance);
    Materiel immobilise = materiels.save(materiel);
    tracer(immobilise, auteur, "Début de l'ordre de travail " + enregistree.getNumeroOt());
    evenements.maintenance(EventTypes.MAINTENANCE_DEMARREE, enregistree, immobilise);
    evenements.materiel(
        EventTypes.MATERIEL_ETAT_CHANGE,
        immobilise,
        chargeur.chantierOuNull(immobilise.getChantierActuelId()),
        "Ordre de travail " + enregistree.getNumeroOt() + " démarré");
    return vues.maintenance(enregistree);
  }

  @Override
  public MaintenanceVue cloturer(CloturerMaintenanceCommande commande) {
    Maintenance maintenance = chargeur.maintenance(commande.maintenanceId());
    Materiel materiel = chargeur.materiel(maintenance.getMaterielId());
    LocalDate aujourdhui = LocalDate.now(clock);
    maintenance.cloturer(
        aujourdhui, commande.cout(), commande.observations(), commande.heuresCompteur());
    if (commande.heuresCompteur() != null) {
      materiel.releverHeures(commande.heuresCompteur());
    }
    materiel.enregistrerMaintenance(aujourdhui);
    // Un autre ordre de travail en cours maintient l'immobilisation.
    if (!maintenances.existeEnCours(materiel.getId(), maintenance.getId())) {
      materiel.remettreEnService(affectations.findActiveByMateriel(materiel.getId()).isPresent());
    }
    Maintenance cloturee = maintenances.save(maintenance);
    Materiel remis = materiels.save(materiel);
    if (cloturee.getPanneId() != null) {
      Panne panne = chargeur.panne(cloturee.getPanneId());
      if (panne.getStatut() == StatutPanne.PRISE_EN_CHARGE) {
        panne.resoudre();
        pannes.save(panne);
      }
    }
    tracer(remis, commande.auteur(), "Fin de l'ordre de travail " + cloturee.getNumeroOt());
    evenements.maintenance(EventTypes.MAINTENANCE_CLOTUREE, cloturee, remis);
    evenements.materiel(
        EventTypes.MATERIEL_ETAT_CHANGE,
        remis,
        chargeur.chantierOuNull(remis.getChantierActuelId()),
        "Ordre de travail " + cloturee.getNumeroOt() + " clôturé");
    return vues.maintenance(cloturee);
  }

  private String numeroOt() {
    int annee = LocalDate.now(clock).getYear();
    return String.format(
        "%s-%d-%04d",
        parametres.numerotation().prefixeOrdreTravail(),
        annee,
        sequences.prochaineValeur(SERIE_OT, annee));
  }

  private void tracer(Materiel materiel, CurrentUser auteur, String commentaire) {
    mouvements.ajouter(
        Mouvement.de(
            materiel.getId(),
            TypeMouvement.MAINTENANCE,
            null,
            null,
            Instant.now(clock),
            Auteurs.de(auteur),
            commentaire));
  }
}
