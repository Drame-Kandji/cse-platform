package sn.cse.besoins.application.port.out;

import java.util.Optional;
import java.util.UUID;
import sn.cse.besoins.domain.model.BesoinMateriel;

/** Persistance de l'agrégat {@link BesoinMateriel} (historique et matériels inclus). */
public interface BesoinRepository {

  /**
   * @param besoin agrégat à enregistrer (création ou mise à jour)
   * @return l'agrégat tel que stocké (version à jour, sans événement en attente)
   */
  BesoinMateriel enregistrer(BesoinMateriel besoin);

  /**
   * @param id identifiant
   * @return l'agrégat, s'il existe
   */
  Optional<BesoinMateriel> trouver(UUID id);

  /**
   * @param id identifiant
   * @return vrai si le besoin existe
   */
  boolean existe(UUID id);
}
