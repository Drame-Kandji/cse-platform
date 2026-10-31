package sn.cse.achats.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.achats.infrastructure.persistence.entity.AppelOffreEntity;

/** Accès Spring Data à la table {@code appel_offre}. */
public interface AppelOffreJpaRepository
    extends JpaRepository<AppelOffreEntity, UUID>, JpaSpecificationExecutor<AppelOffreEntity> {

  /**
   * @param statut statut recherché (PUBLIE)
   * @param maintenant instant de référence
   * @return identifiants des appels d'offres échus
   */
  @Query(
      "select a.id from AppelOffreEntity a where a.statut = :statut and a.dateLimite <= :maintenant")
  List<UUID> idsEchus(@Param("statut") StatutAO statut, @Param("maintenant") Instant maintenant);

  /**
   * @param statut statut (PUBLIE)
   * @param maintenant instant de référence (date limite non atteinte)
   * @param ouvert procédure ouverte à tous (APPEL_OFFRES)
   * @param fournisseurId fournisseur
   * @param pageable pagination
   * @return appels d'offres accessibles au fournisseur
   */
  @Query(
      value =
          "select a from AppelOffreEntity a where a.statut = :statut and a.dateLimite > :maintenant"
              + " and (a.type = :ouvert or exists (select 1 from AppelOffreEntity b join b.invites i"
              + " where b = a and i.fournisseurId = :fournisseurId))",
      countQuery =
          "select count(a) from AppelOffreEntity a where a.statut = :statut and a.dateLimite >"
              + " :maintenant and (a.type = :ouvert or exists (select 1 from AppelOffreEntity b"
              + " join b.invites i where b = a and i.fournisseurId = :fournisseurId))")
  Page<AppelOffreEntity> ouvertsAuFournisseur(
      @Param("statut") StatutAO statut,
      @Param("maintenant") Instant maintenant,
      @Param("ouvert") TypeAppelOffre ouvert,
      @Param("fournisseurId") UUID fournisseurId,
      Pageable pageable);

  /**
   * @param fournisseurId fournisseur
   * @param pageable pagination
   * @return appels d'offres sur lesquels le fournisseur a déposé une offre
   */
  @Query(
      value =
          "select a from AppelOffreEntity a where exists (select 1 from OffreEntity o where"
              + " o.appelOffre = a and o.fournisseurId = :fournisseurId)",
      countQuery =
          "select count(a) from AppelOffreEntity a where exists (select 1 from OffreEntity o"
              + " where o.appelOffre = a and o.fournisseurId = :fournisseurId)")
  Page<AppelOffreEntity> avecOffreDu(@Param("fournisseurId") UUID fournisseurId, Pageable pageable);
}
