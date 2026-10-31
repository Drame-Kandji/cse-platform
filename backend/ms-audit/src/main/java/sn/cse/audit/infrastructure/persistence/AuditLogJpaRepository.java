package sn.cse.audit.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Accès Spring Data en lecture au journal. Étend {@link Repository} (et non {@code JpaRepository})
 * afin qu'aucune méthode de suppression ou de modification ne soit exposée au code.
 */
public interface AuditLogJpaRepository extends Repository<AuditLogEntity, UUID> {

  /**
   * @param eventId identifiant d'événement
   * @return vrai si l'événement est déjà journalisé
   */
  boolean existsByEventId(UUID eventId);

  /**
   * @return nombre total d'entrées
   */
  long count();

  /**
   * @param depuis début inclus
   * @return nombre d'entrées postérieures
   */
  long countByOccurredAtGreaterThanEqual(Instant depuis);

  /**
   * @return couples (service, total) par volume décroissant
   */
  @Query(
      "select a.service as service, count(a) as total from AuditLogEntity a"
          + " group by a.service order by count(a) desc")
  List<TotalServiceVue> totalParService();

  /** Projection du volume par service. */
  interface TotalServiceVue {

    /**
     * @return service émetteur
     */
    String getService();

    /**
     * @return nombre d'entrées
     */
    long getTotal();
  }
}
