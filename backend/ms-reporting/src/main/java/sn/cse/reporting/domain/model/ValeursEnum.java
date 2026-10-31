package sn.cse.reporting.domain.model;

import java.util.Locale;

/** Lecture tolérante des valeurs d'énumération reçues dans les événements. */
public final class ValeursEnum {

  private ValeursEnum() {}

  /**
   * @param type énumération attendue
   * @param valeur valeur reçue (code {@code UPPER_SNAKE})
   * @param <E> type d'énumération
   * @return la constante, ou {@code null} si la valeur est absente
   * @throws IllegalArgumentException si la valeur est inconnue (l'événement part alors en DLT sans
   *     rejeu : le rejouer ne le rendrait pas lisible)
   */
  public static <E extends Enum<E>> E lire(Class<E> type, String valeur) {
    if (valeur == null || valeur.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, valeur.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Valeur « " + valeur + " » inconnue pour " + type.getSimpleName(), e);
    }
  }
}
