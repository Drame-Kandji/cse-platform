package sn.cse.besoins.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.application.query.CriteresRechercheBesoins;
import sn.cse.besoins.domain.model.HistoriqueBesoin;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/** Consultation des besoins (un chef de chantier ne voit que ses chantiers). */
public interface ConsulterBesoinsUseCase {

  /**
   * @param id besoin
   * @param utilisateur lecteur
   * @return le besoin
   */
  BesoinVue obtenir(UUID id, CurrentUser utilisateur);

  /**
   * @param criteres filtres
   * @param page pagination
   * @param utilisateur lecteur
   * @return la page de besoins visibles
   */
  PageResponse<BesoinVue> rechercher(
      CriteresRechercheBesoins criteres, PageQuery page, CurrentUser utilisateur);

  /**
   * @param id besoin
   * @param utilisateur lecteur
   * @return l'historique chronologique
   */
  List<HistoriqueBesoin> historique(UUID id, CurrentUser utilisateur);
}
