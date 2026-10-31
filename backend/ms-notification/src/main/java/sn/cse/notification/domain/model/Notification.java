package sn.cse.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Notification produite à partir d'un événement métier. Immuable : seul l'état de lecture (par
 * utilisateur) évolue, et il est porté par {@link NotificationVue}.
 *
 * @param id identifiant
 * @param type type de l'événement d'origine (ex. {@code BESOIN_SOUMIS})
 * @param titre titre court en français
 * @param message message en français
 * @param niveau criticité
 * @param lien route du frontend, ou {@code null}
 * @param entite type d'entité concernée (ex. {@code BESOIN}), ou {@code null}
 * @param entiteId identifiant de l'entité concernée, ou {@code null}
 * @param destinataire utilisateur ou rôle destinataire
 * @param eventId identifiant de l'événement source
 * @param createdAt date de création
 */
public record Notification(
    UUID id,
    String type,
    String titre,
    String message,
    NiveauNotification niveau,
    String lien,
    String entite,
    String entiteId,
    Destinataire destinataire,
    UUID eventId,
    Instant createdAt) {

  public Notification {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(titre, "titre");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(niveau, "niveau");
    Objects.requireNonNull(destinataire, "destinataire");
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  /**
   * @param utilisateurId utilisateur courant
   * @param roles rôles de l'utilisateur courant
   * @return vrai si la notification lui est destinée (directement ou via un de ses rôles)
   */
  public boolean estDestineeA(UUID utilisateurId, java.util.Set<String> roles) {
    if (destinataire instanceof Destinataire.Utilisateur u) {
      return u.id().equals(utilisateurId);
    }
    return roles.contains(((Destinataire.Role) destinataire).code());
  }
}
