package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.PanneVue;
import sn.cse.suivi.domain.model.StatutPanne;

/** Consultation des pannes. */
public interface ConsulterPannesUseCase {

  /**
   * @param statut statut (optionnel)
   * @param chantierId chantier (optionnel)
   * @param materielId matériel (optionnel)
   * @param page pagination
   * @param lecteur utilisateur (chef : ses chantiers)
   * @return la page de pannes
   */
  PageResponse<PanneVue> rechercher(
      StatutPanne statut, UUID chantierId, UUID materielId, PageQuery page, CurrentUser lecteur);
}
