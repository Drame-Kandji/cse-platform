package sn.cse.audit.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.audit.domain.model.StatistiquesAudit.TotalParService;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/**
 * Port de persistance du journal d'audit. Volontairement limité à l'ajout et à la lecture : aucune
 * opération de modification ni de suppression n'existe (immuabilité, doublée d'un déclencheur
 * PostgreSQL).
 */
public interface JournalAuditRepository {

  /**
   * @param eventId identifiant d'événement
   * @return vrai si une entrée existe déjà pour cet événement
   */
  boolean existePourEvenement(UUID eventId);

  /**
   * @param entree entrée à ajouter
   */
  void ajouter(AuditLog entree);

  /**
   * @param critere filtres
   * @param page pagination
   * @return la page correspondante
   */
  PageResponse<AuditLog> rechercher(CritereRechercheAudit critere, PageQuery page);

  /**
   * @return nombre total d'entrées
   */
  long compterTout();

  /**
   * @param depuis instant de début inclus
   * @return nombre d'entrées dont l'action est postérieure ou égale à {@code depuis}
   */
  long compterDepuis(Instant depuis);

  /**
   * @return volume par service émetteur
   */
  List<TotalParService> totalParService();
}
