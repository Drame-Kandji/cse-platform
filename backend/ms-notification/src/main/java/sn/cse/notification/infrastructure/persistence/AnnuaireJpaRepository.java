package sn.cse.notification.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Accès Spring Data à l'annuaire. */
interface AnnuaireJpaRepository extends JpaRepository<UtilisateurAnnuaireEntity, UUID> {

  @Query(
      "select distinct u from UtilisateurAnnuaireEntity u join u.chantierIds c"
          + " where c = :chantierId and u.role = :role and u.actif = true")
  List<UtilisateurAnnuaireEntity> parChantierEtRole(
      @Param("chantierId") UUID chantierId, @Param("role") String role);

  List<UtilisateurAnnuaireEntity> findByRoleAndActifTrue(String role);
}
