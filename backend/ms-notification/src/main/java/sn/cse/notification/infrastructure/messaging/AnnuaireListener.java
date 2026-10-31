package sn.cse.notification.infrastructure.messaging;

import java.util.HashSet;
import java.util.Set;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.UtilisateurPayload;
import sn.cse.notification.application.port.in.SynchroniserAnnuaireUseCase;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Consommateur idempotent de {@code cse.auth.events} alimentant l'annuaire local. */
@Component
public class AnnuaireListener {

  static final String CONSOMMATEUR = "ms-notification.annuaire";
  private static final Set<String> TYPES =
      Set.of(
          EventTypes.UTILISATEUR_CREE,
          EventTypes.UTILISATEUR_MODIFIE,
          EventTypes.UTILISATEUR_SYNCHRONISE);

  private final EventEnvelopeReader reader;
  private final IdempotentEventHandler idempotence;
  private final SynchroniserAnnuaireUseCase useCase;

  /**
   * @param reader lecture des enveloppes
   * @param idempotence garde « une seule fois »
   * @param useCase synchronisation
   */
  public AnnuaireListener(
      EventEnvelopeReader reader,
      IdempotentEventHandler idempotence,
      SynchroniserAnnuaireUseCase useCase) {
    this.reader = reader;
    this.idempotence = idempotence;
    this.useCase = useCase;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(topics = Topics.AUTH, groupId = "${spring.application.name}")
  public void consommer(String message) {
    EventEnvelope env = reader.lire(message);
    if (!TYPES.contains(env.type())) {
      return;
    }
    UtilisateurPayload p = reader.payload(env, UtilisateurPayload.class);
    idempotence.traiterUneFois(
        env.eventId(),
        CONSOMMATEUR,
        () ->
            useCase.synchroniser(
                new UtilisateurAnnuaire(
                    p.utilisateurId(),
                    p.email(),
                    p.prenom(),
                    p.nom(),
                    p.role(),
                    new HashSet<>(p.chantierIds()),
                    p.actif(),
                    env.occurredAt())));
  }
}
