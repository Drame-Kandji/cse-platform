package sn.cse.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Table {@code notification_lectures} : état « lue » par utilisateur. */
@Entity
@Table(name = "notification_lectures")
@IdClass(LectureId.class)
public class LectureEntity {

  @Id
  @Column(name = "notification_id")
  private UUID notificationId;

  @Id
  @Column(name = "utilisateur_id")
  private UUID utilisateurId;

  @Column(name = "lue_le", nullable = false)
  private Instant lueLe;

  protected LectureEntity() {}
}
