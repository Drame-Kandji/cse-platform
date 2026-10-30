package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.MaintenanceVue;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeMaintenance;

/** Consultation des ordres de travail. */
public interface ConsulterMaintenancesUseCase {

  /**
   * @param statut statut (optionnel)
   * @param type type (optionnel)
   * @param materielId matériel (optionnel)
   * @param page pagination
   * @param lecteur utilisateur (chef : ses chantiers)
   * @return la page d'ordres de travail
   */
  PageResponse<MaintenanceVue> rechercher(
      StatutMaintenance statut,
      TypeMaintenance type,
      UUID materielId,
      PageQuery page,
      CurrentUser lecteur);
}
