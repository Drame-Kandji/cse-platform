package sn.cse.achats.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Fournisseur invité à un appel d'offres (raison sociale figée à l'invitation).
 *
 * @param id identifiant du fournisseur
 * @param raisonSociale raison sociale
 */
public record FournisseurInvite(UUID id, String raisonSociale) {

  public FournisseurInvite {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(raisonSociale, "raisonSociale");
  }
}
