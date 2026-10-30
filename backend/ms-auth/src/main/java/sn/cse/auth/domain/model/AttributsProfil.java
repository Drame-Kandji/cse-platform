package sn.cse.auth.domain.model;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import sn.cse.auth.domain.exception.AttributProfilInvalideException;

/**
 * Attributs spécifiques aux profils, tels qu'ils circulent entre l'API, Keycloak et le domaine.
 * Chaque sous-classe de {@link Utilisateur} ne retient que les attributs qui la concernent.
 *
 * @param chantierIds chantiers attribués (chef de chantier)
 * @param perimetreParc périmètre du parc géré (responsable matériel)
 * @param seuilDelegation seuil de délégation en FCFA (responsable des achats)
 * @param budgetGere budget géré en FCFA (DAF)
 * @param fournisseurId fournisseur rattaché (compte du portail fournisseur)
 */
public record AttributsProfil(
    Set<UUID> chantierIds,
    String perimetreParc,
    BigDecimal seuilDelegation,
    BigDecimal budgetGere,
    UUID fournisseurId) {

  /** Aucun attribut spécifique. */
  public static final AttributsProfil AUCUN = new AttributsProfil(Set.of(), null, null, null, null);

  public AttributsProfil {
    chantierIds = chantierIds == null ? Set.of() : Set.copyOf(chantierIds);
    perimetreParc = perimetreParc == null || perimetreParc.isBlank() ? null : perimetreParc.strip();
    verifierPositif(seuilDelegation, "seuilDelegation");
    verifierPositif(budgetGere, "budgetGere");
  }

  private static void verifierPositif(BigDecimal montant, String champ) {
    if (montant != null && montant.signum() < 0) {
      throw new AttributProfilInvalideException(champ, "le montant ne peut pas être négatif");
    }
  }
}
