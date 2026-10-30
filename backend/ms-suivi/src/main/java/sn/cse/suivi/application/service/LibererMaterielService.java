package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.LibererMaterielCommande;
import sn.cse.suivi.application.port.in.LibererMaterielUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Libération d'un matériel : fin de l'affectation active et retour au parc. */
@Service
@Transactional
public class LibererMaterielService implements LibererMaterielUseCase {

  private final AffectationRepository affectations;
  private final MaterielRepository materiels;
  private final MouvementRepository mouvements;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;
  private final SuiviEventPublisher evenements;
  private final Clock clock;

  /**
   * @param affectations stockage des affectations
   * @param materiels stockage des matériels
   * @param mouvements journal des mouvements
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   * @param evenements publication des événements
   * @param clock horloge
   */
  public LibererMaterielService(
      AffectationRepository affectations,
      MaterielRepository materiels,
      MouvementRepository mouvements,
      ChargeurAgregats chargeur,
      AssembleurVues vues,
      SuiviEventPublisher evenements,
      Clock clock) {
    this.affectations = affectations;
    this.materiels = materiels;
    this.mouvements = mouvements;
    this.chargeur = chargeur;
    this.vues = vues;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public AffectationVue liberer(LibererMaterielCommande commande) {
    Materiel materiel = chargeur.materiel(commande.materielId());
    Affectation affectation =
        affectations
            .findActiveByMateriel(materiel.getId())
            .orElseThrow(
                () ->
                    SuiviErreurs.conflit(
                        "AUCUNE_AFFECTATION_ACTIVE",
                        "Le matériel " + materiel.getCode() + " n'a aucune affectation en cours."));
    LocalDate aujourdhui = LocalDate.now(clock);
    LocalDate fin = commande.dateFinReelle() == null ? aujourdhui : commande.dateFinReelle();
    if (fin.isAfter(aujourdhui)) {
      throw SuiviErreurs.regle(
          "DATE_FIN_INVALIDE", "La date de fin réelle ne peut pas être dans le futur.");
    }
    Chantier chantier = chargeur.chantier(affectation.getChantierId());
    affectation.terminer(fin, commande.commentaire());
    materiel.liberer();
    Affectation terminee = affectations.save(affectation);
    Materiel libere = materiels.save(materiel);
    mouvements.ajouter(
        Mouvement.de(
            libere.getId(),
            TypeMouvement.LIBERATION,
            chantier,
            null,
            Instant.now(clock),
            Auteurs.de(commande.auteur()),
            commande.commentaire()));
    evenements.affectation(EventTypes.AFFECTATION_TERMINEE, terminee, libere, chantier);
    evenements.materiel(
        EventTypes.MATERIEL_ETAT_CHANGE,
        libere,
        null,
        "Libération du chantier " + chantier.getNom());
    return vues.affectation(terminee, libere, chantier);
  }
}
