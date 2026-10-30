package sn.cse.suivi.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.FiltreChantiers;
import sn.cse.suivi.domain.model.Chantier;

/** Stockage des chantiers. */
public interface ChantierRepository {

  /**
   * @param id identifiant
   * @return le chantier s'il existe
   */
  Optional<Chantier> findById(UUID id);

  /**
   * @param chantier chantier à enregistrer (création ou modification versionnée)
   * @return l'état enregistré (version à jour)
   */
  Chantier save(Chantier chantier);

  /**
   * @param code code recherché
   * @param exclureId chantier à ignorer (modification), peut être {@code null}
   * @return vrai si un autre chantier porte ce code
   */
  boolean existsByCode(String code, UUID exclureId);

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page de chantiers
   */
  PageResponse<Chantier> rechercher(FiltreChantiers filtre, PageQuery page);

  /**
   * @param ids identifiants
   * @return les chantiers trouvés
   */
  List<Chantier> findAllById(Collection<UUID> ids);

  /**
   * @return tous les chantiers
   */
  List<Chantier> findAll();
}
