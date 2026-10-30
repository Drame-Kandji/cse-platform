package sn.cse.besoins.application.port.out;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.application.query.CriteresRechercheBesoins;
import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.service.BesoinAConsolider;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures optimisées (listes, consolidation, statistiques) sans reconstituer les agrégats. */
public interface BesoinQueryPort {

  /**
   * @param criteres filtres (restriction de chantiers incluse)
   * @param page pagination et tri validés
   * @return la page de besoins
   */
  PageResponse<BesoinVue> rechercher(CriteresRechercheBesoins criteres, PageQuery page);

  /**
   * @param statuts statuts retenus
   * @param limite nombre maximal de besoins lus
   * @return les besoins à consolider
   */
  List<BesoinAConsolider> aConsolider(Set<StatutBesoin> statuts, int limite);

  /**
   * @param chantiers chantiers retenus, {@code null} pour tous
   * @return les statistiques agrégées
   */
  StatistiquesBesoins statistiques(Set<UUID> chantiers);
}
