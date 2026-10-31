package sn.cse.achats.application.port.out;

import java.util.Optional;
import java.util.UUID;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Port de la projection des besoins transmis aux achats. */
public interface BesoinAAcheterRepository {

  /**
   * @param besoin projection à enregistrer
   */
  void enregistrer(BesoinAAcheter besoin);

  /**
   * @param besoinId identifiant
   * @return la projection si elle existe
   */
  Optional<BesoinAAcheter> parBesoin(UUID besoinId);

  /**
   * @param page pagination
   * @return les besoins à acheter, les plus récents d'abord
   */
  PageResponse<BesoinAAcheter> lister(PageQuery page);
}
