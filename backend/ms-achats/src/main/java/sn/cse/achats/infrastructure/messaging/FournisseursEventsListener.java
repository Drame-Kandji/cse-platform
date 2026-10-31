package sn.cse.achats.infrastructure.messaging;

import java.util.Set;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.achats.application.command.FournisseurObserveCommand;
import sn.cse.achats.application.port.in.MettreAJourProjectionsUseCase;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.FournisseurPayload;

/**
 * Consommateur idempotent de {@code cse.fournisseurs.events} : maintient le statut local des
 * fournisseurs utilisé par la règle R14 (repli si ms-fournisseurs est indisponible, portail).
 */
@Component
public class FournisseursEventsListener {

  static final String CONSOMMATEUR = "ms-achats.statut-fournisseurs";

  /** Événements porteurs d'un {@link FournisseurPayload}. */
  static final Set<String> TYPES =
      Set.of(
          EventTypes.FOURNISSEUR_INSCRIT,
          EventTypes.FOURNISSEUR_VALIDE,
          EventTypes.FOURNISSEUR_SUSPENDU,
          EventTypes.FOURNISSEUR_REACTIVE,
          EventTypes.FOURNISSEUR_BLACKLISTAGE_DEMANDE,
          EventTypes.FOURNISSEUR_BLACKLISTE,
          EventTypes.FOURNISSEUR_BLACKLISTAGE_REFUSE,
          EventTypes.FOURNISSEUR_MODIFIE,
          EventTypes.SCORE_RECALCULE,
          EventTypes.DOCUMENT_EXPIRE);

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final MettreAJourProjectionsUseCase projections;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param projections cas d'utilisation des projections
   */
  public FournisseursEventsListener(
      EventEnvelopeReader lecteur,
      IdempotentEventHandler idempotence,
      MettreAJourProjectionsUseCase projections) {
    this.lecteur = lecteur;
    this.idempotence = idempotence;
    this.projections = projections;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(topics = Topics.FOURNISSEURS, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope env = lecteur.lire(message);
    if (!TYPES.contains(env.type())) {
      return;
    }
    FournisseurPayload p = lecteur.payload(env, FournisseurPayload.class);
    if (p.statut() == null || p.fournisseurId() == null) {
      return;
    }
    FournisseurObserveCommand commande =
        new FournisseurObserveCommand(
            p.fournisseurId(),
            p.raisonSociale(),
            StatutFournisseur.valueOf(p.statut()),
            env.occurredAt());
    idempotence.traiterUneFois(
        env.eventId(), CONSOMMATEUR, () -> projections.fournisseurObserve(commande));
  }
}
