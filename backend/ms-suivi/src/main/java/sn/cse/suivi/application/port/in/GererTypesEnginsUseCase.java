package sn.cse.suivi.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.TypeEngin;

/** Référentiel des types d'engins. */
public interface GererTypesEnginsUseCase {

  /**
   * @return tous les types, par libellé
   */
  List<TypeEngin> lister();

  /**
   * @param donnees données
   * @return le type créé
   */
  TypeEngin creer(DonneesTypeEngin donnees);

  /**
   * Modifie un type ; les nouveaux seuils de maintenance sont répercutés sur ses matériels.
   *
   * @param id type
   * @param donnees nouvelles données
   * @return le type modifié
   */
  TypeEngin modifier(UUID id, DonneesTypeEngin donnees);
}
