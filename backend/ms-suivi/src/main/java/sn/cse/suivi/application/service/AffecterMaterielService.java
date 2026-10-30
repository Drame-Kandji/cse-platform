package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.AffecterMaterielCommande;
import sn.cse.suivi.application.port.in.AffecterMaterielUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeMouvement;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

/**
 * Affectation d'un matériel (R04). Sûre en concurrence : l'affectation est écrite en premier (index
 * unique « une affectation active par matériel »), puis le matériel avec son verrou optimiste.
 */
@Service
@Transactional
public class AffecterMaterielService implements AffecterMaterielUseCase {

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
  public AffecterMaterielService(
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
  public AffectationVue affecter(AffecterMaterielCommande commande) {
    Materiel materiel = chargeur.materiel(commande.materielId());
    Chantier chantier = chargeur.chantier(commande.chantierId());
    Periode periode = new Periode(commande.dateDebut(), commande.dateFinPrevue());
    Affectation affectation =
        PolitiqueAffectation.affecter(
            materiel,
            chantier,
            commande.besoinId(),
            periode,
            affectations.findActivesByMateriel(materiel.getId()));
    Affectation enregistree = affectations.save(affectation);
    Materiel affecte = materiels.save(materiel);
    mouvements.ajouter(
        Mouvement.de(
            affecte.getId(),
            TypeMouvement.AFFECTATION,
            null,
            chantier,
            Instant.now(clock),
            Auteurs.de(commande.auteur()),
            "Affectation du " + periode.debut() + " au " + periode.fin()));
    evenements.affectation(EventTypes.MATERIEL_AFFECTE, enregistree, affecte, chantier);
    evenements.materiel(
        EventTypes.MATERIEL_ETAT_CHANGE, affecte, chantier, "Affectation à " + chantier.getNom());
    return vues.affectation(enregistree, affecte, chantier);
  }
}
