package sn.cse.notification.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.notification.application.port.out.AnnuaireRepository;
import sn.cse.notification.application.port.out.NotificationRepository;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Implémentations en mémoire des ports de persistance. */
final class FakeRepositories {

  private FakeRepositories() {}

  static final class Notifications implements NotificationRepository {
    final List<Notification> stock = new ArrayList<>();
    final Set<String> lectures = new HashSet<>();

    private static String cle(UUID n, UUID u) {
      return n + "/" + u;
    }

    @Override
    public void enregistrer(List<Notification> notifications) {
      stock.addAll(notifications);
    }

    private List<Notification> visibles(UUID uid, Set<String> roles) {
      return stock.stream()
          .filter(n -> n.estDestineeA(uid, roles))
          .sorted(Comparator.comparing(Notification::createdAt).reversed())
          .toList();
    }

    @Override
    public PageResponse<Notification> visibles(
        UUID uid, Set<String> roles, Boolean lue, PageQuery page) {
      List<Notification> tout =
          visibles(uid, roles).stream()
              .filter(n -> lue == null || lue == lectures.contains(cle(n.id(), uid)))
              .toList();
      int debut = Math.min(page.page() * page.size(), tout.size());
      int fin = Math.min(debut + page.size(), tout.size());
      return PageResponse.of(tout.subList(debut, fin), page.page(), page.size(), tout.size());
    }

    @Override
    public Set<UUID> luesParmi(UUID uid, Collection<UUID> ids) {
      return ids.stream().filter(i -> lectures.contains(cle(i, uid))).collect(Collectors.toSet());
    }

    @Override
    public long compterNonLues(UUID uid, Set<String> roles) {
      return visibles(uid, roles).stream()
          .filter(n -> !lectures.contains(cle(n.id(), uid)))
          .count();
    }

    @Override
    public Optional<Notification> parId(UUID id) {
      return stock.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    @Override
    public void marquerLue(UUID n, UUID u, Instant date) {
      lectures.add(cle(n, u));
    }

    @Override
    public int toutLire(UUID uid, Set<String> roles, Instant date) {
      int avant = lectures.size();
      visibles(uid, roles).forEach(n -> lectures.add(cle(n.id(), uid)));
      return lectures.size() - avant;
    }
  }

  static class Annuaire implements AnnuaireRepository {
    final Map<UUID, UtilisateurAnnuaire> stock = new HashMap<>();

    @Override
    public void enregistrer(UtilisateurAnnuaire u) {
      stock.put(u.id(), u);
    }

    @Override
    public Optional<UtilisateurAnnuaire> parId(UUID id) {
      return Optional.ofNullable(stock.get(id));
    }

    @Override
    public List<UtilisateurAnnuaire> chefsDuChantier(UUID chantierId) {
      return stock.values().stream()
          .filter(
              u ->
                  u.actif()
                      && "CHEF_CHANTIER".equals(u.role())
                      && u.chantierIds().contains(chantierId))
          .toList();
    }

    @Override
    public List<UtilisateurAnnuaire> actifsParRole(String role) {
      return stock.values().stream().filter(u -> u.actif() && role.equals(u.role())).toList();
    }
  }
}
