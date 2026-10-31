package sn.cse.notification.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Événement métier reçu, réduit à une vue indépendante de la sérialisation : les champs du payload
 * sont exposés sous forme d'arbre de {@link Map} / {@link List} / valeurs simples.
 *
 * @param eventId identifiant unique
 * @param type type (ex. {@code BESOIN_VALIDE})
 * @param aggregateId identifiant de l'agrégat
 * @param occurredAt date de l'événement
 * @param acteurNom nom de l'auteur de l'action
 * @param donnees champs du payload
 */
public record EvenementMetier(
    UUID eventId,
    String type,
    String aggregateId,
    Instant occurredAt,
    String acteurNom,
    Map<String, Object> donnees) {

  /** Champ virtuel donnant le nom de l'auteur de l'action. */
  public static final String CHAMP_ACTEUR = "acteurNom";

  /** Champ virtuel donnant l'identifiant de l'agrégat. */
  public static final String CHAMP_AGREGAT = "aggregateId";

  public EvenementMetier {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
    donnees = donnees == null ? Map.of() : donnees;
  }

  /**
   * Lit un champ du payload ; les chemins pointés ({@code a.b}) parcourent les objets imbriqués.
   *
   * @param chemin nom du champ
   * @return la valeur, si présente et non nulle
   */
  public Optional<Object> valeur(String chemin) {
    if (CHAMP_ACTEUR.equals(chemin)) {
      return Optional.ofNullable(acteurNom);
    }
    if (CHAMP_AGREGAT.equals(chemin)) {
      return Optional.ofNullable(aggregateId);
    }
    Object courant = donnees;
    for (String segment : chemin.split("\\.")) {
      if (!(courant instanceof Map<?, ?> map)) {
        return Optional.empty();
      }
      courant = map.get(segment);
    }
    return Optional.ofNullable(courant);
  }

  /**
   * @param chemin nom du champ
   * @return la valeur interprétée comme UUID, si elle est présente et valide
   */
  public Optional<UUID> uuid(String chemin) {
    return valeur(chemin)
        .map(String::valueOf)
        .flatMap(
            s -> {
              try {
                return Optional.of(UUID.fromString(s));
              } catch (IllegalArgumentException e) {
                return Optional.empty();
              }
            });
  }
}
