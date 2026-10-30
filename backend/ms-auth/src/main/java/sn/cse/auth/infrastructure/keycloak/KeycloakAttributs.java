package sn.cse.auth.infrastructure.keycloak;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.cse.auth.domain.model.AttributsProfil;

/**
 * Correspondance entre {@link AttributsProfil} et les attributs utilisateur Keycloak (tous
 * multivalués côté Keycloak ; seul {@code chantier_ids} l'est réellement). Ces noms sont repris par
 * les mappers de jetons du realm.
 */
final class KeycloakAttributs {

  static final String CHANTIER_IDS = "chantier_ids";
  static final String PERIMETRE_PARC = "perimetre_parc";
  static final String SEUIL_DELEGATION = "seuil_delegation";
  static final String BUDGET_GERE = "budget_gere";
  static final String FOURNISSEUR_ID = "fournisseur_id";

  /** Attributs gérés par ms-auth (les autres attributs Keycloak sont préservés). */
  static final List<String> GERES =
      List.of(CHANTIER_IDS, PERIMETRE_PARC, SEUIL_DELEGATION, BUDGET_GERE, FOURNISSEUR_ID);

  private static final Logger LOG = LoggerFactory.getLogger(KeycloakAttributs.class);

  private KeycloakAttributs() {}

  /**
   * @param attributs attributs du profil
   * @return les attributs Keycloak non vides
   */
  static Map<String, List<String>> versKeycloak(AttributsProfil attributs) {
    Map<String, List<String>> resultat = new java.util.LinkedHashMap<>();
    if (!attributs.chantierIds().isEmpty()) {
      resultat.put(
          CHANTIER_IDS, attributs.chantierIds().stream().map(UUID::toString).sorted().toList());
    }
    ajouter(resultat, PERIMETRE_PARC, attributs.perimetreParc());
    ajouter(resultat, SEUIL_DELEGATION, montant(attributs.seuilDelegation()));
    ajouter(resultat, BUDGET_GERE, montant(attributs.budgetGere()));
    ajouter(resultat, FOURNISSEUR_ID, attributs.fournisseurId());
    return resultat;
  }

  /**
   * Lecture tolérante : une valeur mal formée est ignorée (journalisée) plutôt que de bloquer la
   * synchronisation de tout l'annuaire.
   *
   * @param brut attributs Keycloak
   * @return les attributs du profil
   */
  static AttributsProfil depuisKeycloak(Map<String, List<String>> brut) {
    Map<String, List<String>> a = brut == null ? Map.of() : brut;
    Set<UUID> chantiers =
        valeurs(a, CHANTIER_IDS).stream()
            .map(KeycloakAttributs::uuid)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
    BigDecimal seuil =
        premiere(a, SEUIL_DELEGATION).flatMap(KeycloakAttributs::nombre).orElse(null);
    BigDecimal budget = premiere(a, BUDGET_GERE).flatMap(KeycloakAttributs::nombre).orElse(null);
    return new AttributsProfil(
        chantiers,
        premiere(a, PERIMETRE_PARC).orElse(null),
        seuil == null || seuil.signum() < 0 ? null : seuil,
        budget == null || budget.signum() < 0 ? null : budget,
        premiere(a, FOURNISSEUR_ID).flatMap(KeycloakAttributs::uuid).orElse(null));
  }

  private static void ajouter(Map<String, List<String>> cible, String nom, Object valeur) {
    if (valeur != null) {
      cible.put(nom, List.of(valeur.toString()));
    }
  }

  private static String montant(BigDecimal valeur) {
    return valeur == null ? null : valeur.toPlainString();
  }

  private static Collection<String> valeurs(Map<String, List<String>> a, String nom) {
    return a.getOrDefault(nom, List.of()).stream()
        .filter(Objects::nonNull)
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  private static Optional<String> premiere(Map<String, List<String>> a, String nom) {
    return valeurs(a, nom).stream().findFirst();
  }

  private static Optional<UUID> uuid(String valeur) {
    try {
      return Optional.of(UUID.fromString(valeur));
    } catch (IllegalArgumentException e) {
      LOG.warn("Attribut Keycloak ignoré : UUID invalide « {} »", valeur);
      return Optional.empty();
    }
  }

  private static Optional<BigDecimal> nombre(String valeur) {
    try {
      return Optional.of(new BigDecimal(valeur));
    } catch (NumberFormatException e) {
      LOG.warn("Attribut Keycloak ignoré : montant invalide « {} »", valeur);
      return Optional.empty();
    }
  }
}
