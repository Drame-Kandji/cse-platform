package sn.cse.fournisseurs.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.infrastructure.persistence.entity.FournisseurEntity;

/** Dépôt Spring Data des fournisseurs. */
public interface FournisseurJpaRepository
    extends JpaRepository<FournisseurEntity, UUID>, JpaSpecificationExecutor<FournisseurEntity> {

  /**
   * @param ninea NINEA
   * @return {@code true} si utilisé
   */
  boolean existsByNinea(String ninea);

  /**
   * @param ninea NINEA
   * @param id identifiant exclu
   * @return {@code true} si utilisé par un autre fournisseur
   */
  boolean existsByNineaAndIdNot(String ninea, UUID id);

  /**
   * @param email adresse e-mail
   * @return {@code true} si utilisée (insensible à la casse)
   */
  boolean existsByEmailIgnoreCase(String email);

  /**
   * @param statut statut
   * @return fournisseurs dans ce statut
   */
  List<FournisseurEntity> findByStatut(StatutFournisseur statut);

  /**
   * @param jour date de référence
   * @return fournisseurs ayant un document expiré non signalé
   */
  @Query(
      "select distinct f from FournisseurEntity f join f.documents d"
          + " where d.dateExpiration < :jour and d.expirationNotifiee = false")
  List<FournisseurEntity> avecDocumentsExpiresNonSignales(@Param("jour") LocalDate jour);

  /**
   * @return couples [statut, nombre]
   */
  @Query("select f.statut, count(f) from FournisseurEntity f group by f.statut")
  List<Object[]> compterParStatut();

  /**
   * @param jour date de référence
   * @return nombre de documents expirés
   */
  @Query("select count(d) from DocumentEntity d where d.dateExpiration < :jour")
  long compterDocumentsExpires(@Param("jour") LocalDate jour);
}
