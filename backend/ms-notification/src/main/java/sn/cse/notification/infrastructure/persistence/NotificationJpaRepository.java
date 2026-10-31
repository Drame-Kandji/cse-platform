package sn.cse.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Accès Spring Data aux notifications. */
interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

  String VISIBLE = "(n.destinataireId = :uid or n.destinataireRole in :roles)";
  String LUE =
      "exists (select 1 from LectureEntity l where l.notificationId = n.id"
          + " and l.utilisateurId = :uid)";

  @Query("select n from NotificationEntity n where " + VISIBLE)
  Page<NotificationEntity> visibles(
      @Param("uid") UUID uid, @Param("roles") Collection<String> roles, Pageable pageable);

  @Query("select n from NotificationEntity n where " + VISIBLE + " and " + LUE)
  Page<NotificationEntity> lues(
      @Param("uid") UUID uid, @Param("roles") Collection<String> roles, Pageable pageable);

  @Query("select n from NotificationEntity n where " + VISIBLE + " and not " + LUE)
  Page<NotificationEntity> nonLues(
      @Param("uid") UUID uid, @Param("roles") Collection<String> roles, Pageable pageable);

  @Query("select count(n) from NotificationEntity n where " + VISIBLE + " and not " + LUE)
  long compterNonLues(@Param("uid") UUID uid, @Param("roles") Collection<String> roles);

  @Query(
      "select l.notificationId from LectureEntity l where l.utilisateurId = :uid"
          + " and l.notificationId in :ids")
  List<UUID> luesParmi(@Param("uid") UUID uid, @Param("ids") Collection<UUID> ids);

  @Modifying
  @Query(
      value =
          "INSERT INTO notification_lectures (notification_id, utilisateur_id, lue_le)"
              + " VALUES (:id, :uid, :date) ON CONFLICT DO NOTHING",
      nativeQuery = true)
  int marquerLue(@Param("id") UUID id, @Param("uid") UUID uid, @Param("date") Instant date);

  @Modifying
  @Query(
      value =
          "INSERT INTO notification_lectures (notification_id, utilisateur_id, lue_le)"
              + " SELECT n.id, :uid, :date FROM notifications n"
              + " WHERE (n.destinataire_id = :uid OR n.destinataire_role IN (:roles))"
              + " ON CONFLICT DO NOTHING",
      nativeQuery = true)
  int toutLire(
      @Param("uid") UUID uid,
      @Param("roles") Collection<String> roles,
      @Param("date") Instant date);
}
