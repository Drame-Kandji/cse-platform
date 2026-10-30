package sn.cse.fournisseurs.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.fournisseurs.application.query.CompteurLitiges;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.StatutLitige;

/** Port de persistance des litiges. */
public interface LitigeRepository {

  /**
   * @param litige litige à persister
   * @return litige persisté (nouvelle version)
   */
  Litige sauvegarder(Litige litige);

  /**
   * @param id identifiant
   * @return litige
   */
  Optional<Litige> parId(UUID id);

  /**
   * @param statut filtre statut (optionnel)
   * @param fournisseurId filtre fournisseur (optionnel)
   * @param page pagination
   * @return page de litiges
   */
  PageResponse<Litige> rechercher(StatutLitige statut, UUID fournisseurId, PageQuery page);

  /**
   * @param fournisseurIds fournisseurs
   * @return compteurs par fournisseur (absent = aucun litige)
   */
  Map<UUID, CompteurLitiges> compterParFournisseur(Collection<UUID> fournisseurIds);

  /**
   * @return nombre de litiges OUVERT ou EN_COURS
   */
  long compterOuverts();
}
