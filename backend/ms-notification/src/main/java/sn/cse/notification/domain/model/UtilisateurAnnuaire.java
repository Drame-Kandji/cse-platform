package sn.cse.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entrée de l'annuaire local alimenté par {@code cse.auth.events} : permet d'adresser les e-mails
 * et de retrouver les chefs d'un chantier sans appel synchrone à ms-auth.
 *
 * @param id identifiant Keycloak
 * @param email adresse e-mail
 * @param prenom prénom
 * @param nom nom
 * @param role rôle principal
 * @param chantierIds chantiers attribués (chefs de chantier)
 * @param actif compte actif
 * @param misAJourLe date de l'événement ayant produit cette version
 */
public record UtilisateurAnnuaire(
    UUID id,
    String email,
    String prenom,
    String nom,
    String role,
    Set<UUID> chantierIds,
    boolean actif,
    Instant misAJourLe) {

  public UtilisateurAnnuaire {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(misAJourLe, "misAJourLe");
    chantierIds = chantierIds == null ? Set.of() : Set.copyOf(chantierIds);
  }

  /**
   * @return « Prénom Nom » (ou l'e-mail à défaut)
   */
  public String nomComplet() {
    String complet =
        ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom))
            .trim()
            .replaceAll(" +", " ");
    return complet.isEmpty() ? email : complet;
  }

  /**
   * @return vrai si un e-mail peut lui être envoyé
   */
  public boolean joignableParEmail() {
    return actif && email != null && !email.isBlank();
  }

  /**
   * Les événements Kafka peuvent arriver dans le désordre (rejeu) : une version plus ancienne ne
   * doit pas écraser une version plus récente.
   *
   * @param autre version candidate
   * @return vrai si {@code autre} est plus récente ou de même date
   */
  public boolean estRemplaceePar(UtilisateurAnnuaire autre) {
    return !autre.misAJourLe().isBefore(misAJourLe);
  }
}
