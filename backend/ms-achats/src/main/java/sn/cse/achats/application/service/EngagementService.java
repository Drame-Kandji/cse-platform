package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.application.port.in.ValiderEngagementUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.events.EventTypes;

/** Décision de la DAF sur les engagements (préalable à toute livraison, R06). */
@Service
@Transactional
public class EngagementService implements ValiderEngagementUseCase {

  private final ContratRepository contrats;
  private final AchatsEventPublisher evenements;
  private final Clock clock;

  /**
   * @param contrats dépôt des contrats
   * @param evenements publication d'événements
   * @param clock horloge
   */
  public EngagementService(
      ContratRepository contrats, AchatsEventPublisher evenements, Clock clock) {
    this.contrats = contrats;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public ContratAchat decider(DecisionCommand commande) {
    ContratAchat contrat = Chargements.contrat(contrats, commande.cibleId());
    contrat.deciderEngagement(
        commande.approuve(),
        commande.commentaire(),
        Acteurs.de(commande.decideur()),
        Instant.now(clock));
    ContratAchat enregistre = contrats.enregistrer(contrat);
    evenements.contrat(
        commande.approuve() ? EventTypes.ENGAGEMENT_VALIDE : EventTypes.ENGAGEMENT_REJETE,
        enregistre);
    return enregistre;
  }
}
