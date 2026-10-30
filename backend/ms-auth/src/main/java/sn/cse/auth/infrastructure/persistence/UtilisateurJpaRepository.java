package sn.cse.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Accès Spring Data aux profils. */
public interface UtilisateurJpaRepository
    extends JpaRepository<UtilisateurEntity, UUID>, JpaSpecificationExecutor<UtilisateurEntity> {

  /**
   * @param email adresse e-mail
   * @return le profil portant cette adresse (insensible à la casse)
   */
  Optional<UtilisateurEntity> findByEmailIgnoreCase(String email);

  /**
   * @return paires (rôle, effectif)
   */
  @Query("select u.role, count(u) from UtilisateurEntity u group by u.role")
  List<Object[]> compterParRole();

  /**
   * Change la sous-classe d'un profil en SINGLE_TABLE : JPA ne sait pas changer le type d'une
   * entité gérée, on réécrit donc le discriminateur et on efface les attributs de l'ancien profil.
   * Le contexte de persistance est vidé pour que la relecture instancie la nouvelle sous-classe.
   *
   * @param id profil
   * @param role nouveau discriminateur
   * @return nombre de lignes modifiées
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          "UPDATE utilisateurs SET role = :role, chantier_ids = NULL, perimetre_parc = NULL,"
              + " seuil_delegation = NULL, budget_gere = NULL, fournisseur_id = NULL,"
              + " version = version + 1 WHERE id = :id",
      nativeQuery = true)
  int changerRole(@Param("id") UUID id, @Param("role") String role);
}
