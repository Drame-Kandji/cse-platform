package sn.cse.notification.infrastructure.persistence;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.common.security.Roles;
import sn.cse.notification.application.port.out.AnnuaireRepository;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Adaptateur JPA du port {@link AnnuaireRepository}. */
@Component
class AnnuairePersistenceAdapter implements AnnuaireRepository {

  private final AnnuaireJpaRepository jpa;
  private final Clock clock;

  AnnuairePersistenceAdapter(AnnuaireJpaRepository jpa, Clock clock) {
    this.jpa = jpa;
    this.clock = clock;
  }

  @Override
  public void enregistrer(UtilisateurAnnuaire utilisateur) {
    UtilisateurAnnuaireEntity entite =
        jpa.findById(utilisateur.id())
            .orElseGet(() -> new UtilisateurAnnuaireEntity(utilisateur.id(), clock.instant()));
    entite.appliquer(utilisateur, clock.instant());
    jpa.save(entite);
  }

  @Override
  public Optional<UtilisateurAnnuaire> parId(UUID id) {
    return jpa.findById(id).map(UtilisateurAnnuaireEntity::versDomaine);
  }

  @Override
  public List<UtilisateurAnnuaire> chefsDuChantier(UUID chantierId) {
    return jpa.parChantierEtRole(chantierId, Roles.CHEF_CHANTIER).stream()
        .map(UtilisateurAnnuaireEntity::versDomaine)
        .toList();
  }

  @Override
  public List<UtilisateurAnnuaire> actifsParRole(String role) {
    return jpa.findByRoleAndActifTrue(role).stream()
        .map(UtilisateurAnnuaireEntity::versDomaine)
        .toList();
  }
}
