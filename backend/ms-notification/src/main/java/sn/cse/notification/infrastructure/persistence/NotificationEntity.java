package sn.cse.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;

/** Table {@code notifications} (immuable après insertion). */
@Entity
@Table(name = "notifications")
public class NotificationEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 80)
  private String type;

  @Column(nullable = false, length = 200)
  private String titre;

  @Column(nullable = false, columnDefinition = "text")
  private String message;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "niveau_notification")
  private NiveauNotification niveau;

  @Column(length = 300)
  private String lien;

  @Column(length = 40)
  private String entite;

  @Column(name = "entite_id", length = 80)
  private String entiteId;

  @Column(name = "destinataire_id")
  private UUID destinataireId;

  @Column(name = "destinataire_role", length = 40)
  private String destinataireRole;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected NotificationEntity() {}

  static NotificationEntity depuis(Notification n) {
    NotificationEntity e = new NotificationEntity();
    e.id = n.id();
    e.type = n.type();
    e.titre = n.titre();
    e.message = n.message();
    e.niveau = n.niveau();
    e.lien = n.lien();
    e.entite = n.entite();
    e.entiteId = n.entiteId();
    if (n.destinataire() instanceof Destinataire.Utilisateur u) {
      e.destinataireId = u.id();
    } else {
      e.destinataireRole = ((Destinataire.Role) n.destinataire()).code();
    }
    e.eventId = n.eventId();
    e.createdAt = n.createdAt();
    return e;
  }

  Notification versDomaine() {
    Destinataire d =
        destinataireId != null
            ? Destinataire.utilisateur(destinataireId)
            : Destinataire.role(destinataireRole);
    return new Notification(
        id, type, titre, message, niveau, lien, entite, entiteId, d, eventId, createdAt);
  }
}
