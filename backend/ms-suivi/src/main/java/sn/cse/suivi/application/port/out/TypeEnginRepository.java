package sn.cse.suivi.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.suivi.domain.model.TypeEngin;

/** Stockage du référentiel des types d'engins. */
public interface TypeEnginRepository {

  /**
   * @return tous les types, par libellé
   */
  List<TypeEngin> findAll();

  /**
   * @param id identifiant
   * @return le type s'il existe
   */
  Optional<TypeEngin> findById(UUID id);

  /**
   * @param type type à enregistrer
   * @return l'état enregistré
   */
  TypeEngin save(TypeEngin type);

  /**
   * @param code code
   * @param exclureId type à ignorer, peut être {@code null}
   * @return vrai si un autre type porte ce code
   */
  boolean existsByCode(String code, UUID exclureId);
}
