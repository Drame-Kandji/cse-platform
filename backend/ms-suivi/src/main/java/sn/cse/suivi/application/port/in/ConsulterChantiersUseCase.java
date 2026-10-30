package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.domain.model.StatutChantier;

/** Consultation des chantiers (un chef de chantier ne voit que les siens). */
public interface ConsulterChantiersUseCase {

  /**
   * @param statut statut (optionnel)
   * @param q texte libre (optionnel)
   * @param page pagination
   * @param lecteur utilisateur
   * @return la page de chantiers visibles
   */
  PageResponse<ChantierVue> rechercher(
      StatutChantier statut, String q, PageQuery page, CurrentUser lecteur);

  /**
   * @param id chantier
   * @param lecteur utilisateur
   * @return le chantier
   */
  ChantierVue consulter(UUID id, CurrentUser lecteur);
}
