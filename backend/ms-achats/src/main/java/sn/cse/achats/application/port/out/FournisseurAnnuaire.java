package sn.cse.achats.application.port.out;

import java.util.Optional;
import java.util.UUID;
import sn.cse.achats.domain.model.FournisseurConnu;

/**
 * Vérification d'un fournisseur auprès de ms-fournisseurs (source de vérité), avec repli sur la
 * projection locale si le service est indisponible.
 */
public interface FournisseurAnnuaire {

  /**
   * @param id fournisseur
   * @return son état courant, vide s'il est inconnu
   */
  Optional<FournisseurConnu> verifier(UUID id);
}
