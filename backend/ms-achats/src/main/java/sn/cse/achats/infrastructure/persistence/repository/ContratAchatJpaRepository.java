package sn.cse.achats.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.achats.infrastructure.persistence.entity.ContratAchatEntity;

/** Accès Spring Data à la table {@code contrat_achat}. */
public interface ContratAchatJpaRepository
    extends JpaRepository<ContratAchatEntity, UUID>, JpaSpecificationExecutor<ContratAchatEntity> {

  /**
   * @param appelOffreId appel d'offres
   * @return le contrat issu de l'appel d'offres
   */
  Optional<ContratAchatEntity> findByAppelOffreId(UUID appelOffreId);

  /** Projection légère : contrat et appel d'offres d'origine. */
  interface ContratParAppelOffre {

    /**
     * @return identifiant du contrat
     */
    UUID getId();

    /**
     * @return appel d'offres d'origine
     */
    UUID getAppelOffreId();
  }

  /**
   * @param appelOffreIds appels d'offres
   * @return les contrats issus de ces appels d'offres (identifiants seulement)
   */
  @Query(
      "select c.id as id, c.appelOffreId as appelOffreId from ContratAchatEntity c"
          + " where c.appelOffreId in :appelOffreIds")
  List<ContratParAppelOffre> findIdsByAppelOffreIdIn(
      @Param("appelOffreIds") Collection<UUID> appelOffreIds);

  /**
   * @param statuts statuts d'exécution
   * @param date date de référence
   * @return contrats dont l'échéance de livraison est antérieure à la date
   */
  List<ContratAchatEntity> findByStatutInAndDateLivraisonPrevueBefore(
      Collection<StatutContrat> statuts, LocalDate date);
}
