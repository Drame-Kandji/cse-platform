package sn.cse.achats.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import sn.cse.achats.domain.model.FournisseurConnu;

/** Port de la projection locale des fournisseurs (statut R14), alimentée par ms-fournisseurs. */
public interface FournisseurProjectionRepository {

  /**
   * @param id fournisseur
   * @return le fournisseur connu localement
   */
  Optional<FournisseurConnu> parId(UUID id);

  /**
   * Enregistre l'état d'un fournisseur s'il est plus récent que celui déjà connu (les événements
   * peuvent arriver dans le désordre).
   *
   * @param fournisseur état
   * @param observeLe instant de l'observation (date de l'événement ou de l'appel REST)
   */
  void enregistrerSiPlusRecent(FournisseurConnu fournisseur, Instant observeLe);
}
