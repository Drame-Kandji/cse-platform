package sn.cse.auth.infrastructure.messaging;

import java.util.List;
import org.springframework.stereotype.Component;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.UtilisateurPayload;

/** Publie les événements {@code UTILISATEUR_*} via l'outbox transactionnelle. */
@Component
public class OutboxUtilisateurEventPublisher implements UtilisateurEventPublisher {

  static final String AGREGAT = "UTILISATEUR";

  private final OutboxWriter outbox;

  /**
   * @param outbox écrivain d'outbox (cse-common)
   */
  public OutboxUtilisateurEventPublisher(OutboxWriter outbox) {
    this.outbox = outbox;
  }

  @Override
  public void publierCree(Utilisateur utilisateur) {
    publier(EventTypes.UTILISATEUR_CREE, utilisateur, "Création du compte");
  }

  @Override
  public void publierModifie(Utilisateur utilisateur) {
    publier(EventTypes.UTILISATEUR_MODIFIE, utilisateur, "Modification du compte");
  }

  @Override
  public void publierSynchronise(Utilisateur utilisateur) {
    publier(EventTypes.UTILISATEUR_SYNCHRONISE, utilisateur, "Synchronisation du compte");
  }

  private void publier(String type, Utilisateur u, String action) {
    UtilisateurPayload payload =
        new UtilisateurPayload(
            u.id(),
            u.email(),
            u.prenom(),
            u.nom(),
            u.role().code(),
            u.actif(),
            List.copyOf(u.attributs().chantierIds()));
    outbox.publier(
        Topics.AUTH,
        type,
        AGREGAT,
        u.id(),
        payload,
        action + " " + u.nomComplet() + " (" + u.role().code() + ")");
  }
}
