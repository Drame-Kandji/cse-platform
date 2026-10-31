package sn.cse.notification.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clé composite d'une lecture (notification, utilisateur). */
public class LectureId implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID notificationId;
  private UUID utilisateurId;

  protected LectureId() {}

  LectureId(UUID notificationId, UUID utilisateurId) {
    this.notificationId = notificationId;
    this.utilisateurId = utilisateurId;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof LectureId autre
        && Objects.equals(notificationId, autre.notificationId)
        && Objects.equals(utilisateurId, autre.utilisateurId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(notificationId, utilisateurId);
  }
}
