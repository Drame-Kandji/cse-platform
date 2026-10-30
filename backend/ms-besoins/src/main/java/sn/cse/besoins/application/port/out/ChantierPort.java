package sn.cse.besoins.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Consultation des chantiers gérés par ms-suivi. */
public interface ChantierPort {

  /**
   * @param chantierId chantier recherché
   * @return le chantier, vide s'il n'existe pas
   * @throws sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException si ms-suivi ne
   *     répond pas
   */
  Optional<ChantierInfo> trouver(UUID chantierId);

  /**
   * Informations utiles d'un chantier.
   *
   * @param id identifiant
   * @param nom nom
   * @param actif vrai si le chantier est en cours
   */
  record ChantierInfo(UUID id, String nom, boolean actif) {}
}
