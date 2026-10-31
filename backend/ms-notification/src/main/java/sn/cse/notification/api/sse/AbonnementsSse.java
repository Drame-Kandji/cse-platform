package sn.cse.notification.api.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sn.cse.common.security.CurrentUser;
import sn.cse.notification.api.dto.NotificationDto;
import sn.cse.notification.application.port.out.NotificationDiffuseur;
import sn.cse.notification.domain.model.Notification;

/**
 * Registre des connexions SSE (une liste par utilisateur, bornée) ; adaptateur entrant/sortant du
 * port {@link NotificationDiffuseur}. Les connexions mortes sont retirées sur fin, délai dépassé,
 * erreur ou échec d'écriture.
 */
@Component
public class AbonnementsSse implements NotificationDiffuseur {

  static final String EVENEMENT_NOTIFICATION = "notification";
  static final String EVENEMENT_PING = "ping";
  private static final Logger LOG = LoggerFactory.getLogger(AbonnementsSse.class);

  private final Map<UUID, List<Abonnement>> parUtilisateur = new ConcurrentHashMap<>();
  private final SseProperties config;

  /**
   * @param config configuration du flux
   */
  public AbonnementsSse(SseProperties config) {
    this.config = config;
  }

  /** Connexion d'un utilisateur, avec ses rôles au moment de la connexion. */
  private record Abonnement(SseEmitter emitter, Set<String> roles) {}

  /**
   * @param utilisateur utilisateur courant
   * @return un nouveau flux (la plus ancienne connexion est fermée au-delà du maximum)
   */
  public SseEmitter abonner(CurrentUser utilisateur) {
    SseEmitter emitter = new SseEmitter(config.timeout().toMillis());
    Abonnement abonnement = new Abonnement(emitter, utilisateur.roles());
    List<Abonnement> liste =
        parUtilisateur.computeIfAbsent(utilisateur.id(), k -> new CopyOnWriteArrayList<>());
    liste.add(abonnement);
    while (liste.size() > config.maxConnexionsParUtilisateur()) {
      Abonnement ancien = liste.remove(0);
      ancien.emitter().complete();
    }
    Runnable retrait = () -> retirer(utilisateur.id(), abonnement);
    emitter.onCompletion(retrait);
    emitter.onTimeout(retrait);
    emitter.onError(e -> retrait.run());
    // Premier ping immédiat : ouvre le flux côté client et transmet le délai de reconnexion.
    envoyer(
        utilisateur.id(),
        abonnement,
        SseEmitter.event()
            .name(EVENEMENT_PING)
            .reconnectTime(config.reconnexion().toMillis())
            .data("connecte"));
    return emitter;
  }

  @Override
  public int diffuser(Notification notification) {
    NotificationDto dto = NotificationDto.depuis(notification, false);
    int envois = 0;
    for (Map.Entry<UUID, List<Abonnement>> entree : parUtilisateur.entrySet()) {
      for (Abonnement a : entree.getValue()) {
        if (notification.estDestineeA(entree.getKey(), a.roles())
            && envoyer(
                entree.getKey(),
                a,
                SseEmitter.event()
                    .name(EVENEMENT_NOTIFICATION)
                    .id(notification.id().toString())
                    .data(dto, MediaType.APPLICATION_JSON))) {
          envois++;
        }
      }
    }
    return envois;
  }

  /** Maintient les connexions ouvertes à travers les proxys et détecte les clients partis. */
  @Scheduled(
      fixedRateString = "${cse.notification.sse.ping-interval}",
      initialDelayString = "${cse.notification.sse.ping-interval}")
  public void ping() {
    parUtilisateur.forEach(
        (id, liste) ->
            liste.forEach(
                a -> envoyer(id, a, SseEmitter.event().name(EVENEMENT_PING).data("ping"))));
  }

  /**
   * @return nombre de connexions ouvertes
   */
  public int connexions() {
    return parUtilisateur.values().stream().mapToInt(List::size).sum();
  }

  private boolean envoyer(UUID utilisateurId, Abonnement a, SseEmitter.SseEventBuilder evt) {
    try {
      a.emitter().send(evt);
      return true;
    } catch (IOException | IllegalStateException e) {
      LOG.debug("Connexion SSE fermée pour {} : {}", utilisateurId, e.getMessage());
      retirer(utilisateurId, a);
      return false;
    }
  }

  private void retirer(UUID utilisateurId, Abonnement a) {
    parUtilisateur.computeIfPresent(
        utilisateurId,
        (k, liste) -> {
          liste.remove(a);
          return liste.isEmpty() ? null : liste;
        });
  }
}
