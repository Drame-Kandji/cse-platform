package sn.cse.audit.application.port.in;

import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.audit.domain.model.StatistiquesAudit;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Cas d'utilisation : consultation du journal d'audit (lecture seule, administrateur, R12). */
public interface ConsulterAuditUseCase {

  /**
   * @param critere filtres
   * @param page pagination bornée et tri validé
   * @return la page d'entrées correspondantes
   */
  PageResponse<AuditLog> rechercher(CritereRechercheAudit critere, PageQuery page);

  /**
   * @return les statistiques globales du journal
   */
  StatistiquesAudit statistiques();
}
