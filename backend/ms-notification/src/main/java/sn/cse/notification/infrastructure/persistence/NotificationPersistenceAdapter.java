package sn.cse.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.notification.application.port.out.NotificationRepository;
import sn.cse.notification.domain.model.Notification;

/** Adaptateur JPA du port {@link NotificationRepository}. */
@Component
class NotificationPersistenceAdapter implements NotificationRepository {

  /** Tri imposé : les plus récentes d'abord (puis id pour une pagination stable). */
  private static final Sort TRI = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

  private final NotificationJpaRepository jpa;

  NotificationPersistenceAdapter(NotificationJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void enregistrer(List<Notification> notifications) {
    jpa.saveAll(notifications.stream().map(NotificationEntity::depuis).toList());
  }

  @Override
  public PageResponse<Notification> visibles(
      UUID utilisateurId, Set<String> roles, Boolean lue, PageQuery page) {
    Pageable pageable = PageRequest.of(page.page(), page.size(), TRI);
    Collection<String> r = rolesOuAucun(roles);
    Page<NotificationEntity> resultat;
    if (lue == null) {
      resultat = jpa.visibles(utilisateurId, r, pageable);
    } else if (lue) {
      resultat = jpa.lues(utilisateurId, r, pageable);
    } else {
      resultat = jpa.nonLues(utilisateurId, r, pageable);
    }
    return PageResponse.of(
        resultat.getContent().stream().map(NotificationEntity::versDomaine).toList(),
        page.page(),
        page.size(),
        resultat.getTotalElements());
  }

  @Override
  public Set<UUID> luesParmi(UUID utilisateurId, Collection<UUID> notificationIds) {
    if (notificationIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(jpa.luesParmi(utilisateurId, notificationIds));
  }

  @Override
  public long compterNonLues(UUID utilisateurId, Set<String> roles) {
    return jpa.compterNonLues(utilisateurId, rolesOuAucun(roles));
  }

  @Override
  public Optional<Notification> parId(UUID notificationId) {
    return jpa.findById(notificationId).map(NotificationEntity::versDomaine);
  }

  @Override
  public void marquerLue(UUID notificationId, UUID utilisateurId, Instant date) {
    jpa.marquerLue(notificationId, utilisateurId, date);
  }

  @Override
  public int toutLire(UUID utilisateurId, Set<String> roles, Instant date) {
    return jpa.toutLire(utilisateurId, rolesOuAucun(roles), date);
  }

  // Une clause IN vide n'est pas portable : un utilisateur sans rôle ne correspond à aucun rôle
  // (la chaîne vide n'est jamais un code de rôle valide).
  private static Collection<String> rolesOuAucun(Set<String> roles) {
    return roles.isEmpty() ? List.of("") : roles;
  }
}
