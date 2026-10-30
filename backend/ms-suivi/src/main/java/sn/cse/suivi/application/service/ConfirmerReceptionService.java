package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.ConfirmerReceptionCommande;
import sn.cse.suivi.application.port.in.ConfirmerReceptionUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Confirmation de réception : un chef de chantier ne peut confirmer que sur ses chantiers. */
@Service
@Transactional
public class ConfirmerReceptionService implements ConfirmerReceptionUseCase {

  private final AffectationRepository affectations;
  private final MouvementRepository mouvements;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;
  private final SuiviEventPublisher evenements;
  private final Clock clock;

  /**
   * @param affectations stockage des affectations
   * @param mouvements journal des mouvements
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   * @param evenements publication des événements
   * @param clock horloge
   */
  public ConfirmerReceptionService(
      AffectationRepository affectations,
      MouvementRepository mouvements,
      ChargeurAgregats chargeur,
      AssembleurVues vues,
      SuiviEventPublisher evenements,
      Clock clock) {
    this.affectations = affectations;
    this.mouvements = mouvements;
    this.chargeur = chargeur;
    this.vues = vues;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public AffectationVue confirmer(ConfirmerReceptionCommande commande) {
    Materiel materiel = chargeur.materiel(commande.materielId());
    Affectation affectation = affectationVisee(commande, materiel);
    if (!commande.auteur().peutAccederAuChantier(affectation.getChantierId())) {
      throw SuiviErreurs.chantierNonAutorise(affectation.getChantierId());
    }
    Chantier chantier = chargeur.chantier(affectation.getChantierId());
    Instant maintenant = Instant.now(clock);
    affectation.confirmerReception(maintenant, commande.observations());
    Affectation enregistree = affectations.save(affectation);
    mouvements.ajouter(
        Mouvement.de(
            materiel.getId(),
            TypeMouvement.RECEPTION,
            null,
            chantier,
            maintenant,
            Auteurs.de(commande.auteur()),
            commande.observations()));
    evenements.affectation(EventTypes.RECEPTION_CONFIRMEE, enregistree, materiel, chantier);
    return vues.affectation(enregistree, materiel, chantier);
  }

  private Affectation affectationVisee(ConfirmerReceptionCommande commande, Materiel materiel) {
    if (commande.affectationId() == null) {
      return affectations
          .findActiveByMateriel(materiel.getId())
          .orElseThrow(
              () ->
                  SuiviErreurs.conflit(
                      "AUCUNE_AFFECTATION_ACTIVE",
                      "Le matériel " + materiel.getCode() + " n'a aucune affectation en cours."));
    }
    Affectation affectation = chargeur.affectation(commande.affectationId());
    if (!affectation.getMaterielId().equals(materiel.getId())) {
      throw new ResourceNotFoundException("Affectation", commande.affectationId());
    }
    return affectation;
  }
}
