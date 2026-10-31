package sn.cse.notification.application.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.notification.domain.model.Notification;

/** Persistance des notifications et de leur état de lecture par utilisateur. */
public interface NotificationRepository {

  /**
   * @param notifications notifications à enregistrer
   */
  void enregistrer(List<Notification> notifications);

  /**
   * @param utilisateurId utilisateur
   * @param roles rôles de l'utilisateur
   * @param lue filtre de lecture ({@code null} = toutes)
   * @param page pagination (tri : date de création décroissante)
   * @return les notifications visibles
   */
  PageResponse<Notification> visibles(
      UUID utilisateurId, Set<String> roles, Boolean lue, PageQuery page);

  /**
   * @param utilisateurId utilisateur
   * @param notificationIds notifications candidates
   * @return celles que l'utilisateur a lues
   */
  Set<UUID> luesParmi(UUID utilisateurId, Collection<UUID> notificationIds);

  /**
   * @param utilisateurId utilisateur
   * @param roles rôles
   * @return nombre de notifications visibles non lues
   */
  long compterNonLues(UUID utilisateurId, Set<String> roles);

  /**
   * @param notificationId notification
   * @return la notification, si elle existe
   */
  Optional<Notification> parId(UUID notificationId);

  /**
   * Idempotent : marquer deux fois ne crée pas de doublon.
   *
   * @param notificationId notification
   * @param utilisateurId lecteur
   * @param date date de lecture
   */
  void marquerLue(UUID notificationId, UUID utilisateurId, Instant date);

  /**
   * @param utilisateurId lecteur
   * @param roles rôles du lecteur
   * @param date date de lecture
   * @return nombre de lectures ajoutées
   */
  int toutLire(UUID utilisateurId, Set<String> roles, Instant date);
}
