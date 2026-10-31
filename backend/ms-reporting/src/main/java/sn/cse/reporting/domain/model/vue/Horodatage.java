package sn.cse.reporting.domain.model.vue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Règles d'ordonnancement des événements. Kafka garantit l'ordre par agrégat (clé), pas entre
 * agrégats ni entre topics : chaque vue garde l'instant du dernier événement appliqué et ignore un
 * instantané plus ancien.
 */
public final class Horodatage {

  private Horodatage() {}

  /**
   * @param recu instant de l'événement reçu
   * @param connu instant du dernier événement appliqué (null si aucun)
   * @return vrai si l'événement reçu doit être appliqué (égalité incluse : deux événements d'une
   *     même transaction peuvent partager l'instant)
   */
  public static boolean plusRecent(Instant recu, Instant connu) {
    return connu == null || (recu != null && !recu.isBefore(connu));
  }

  /**
   * @param a premier instant (peut être null)
   * @param b second instant (peut être null)
   * @return le plus ancien des instants non nuls
   */
  public static Instant min(Instant a, Instant b) {
    if (a == null) {
      return b;
    }
    return b == null || a.isBefore(b) ? a : b;
  }

  /**
   * @param a premier instant (peut être null)
   * @param b second instant (peut être null)
   * @return le plus récent des instants non nuls
   */
  public static Instant max(Instant a, Instant b) {
    if (a == null) {
      return b;
    }
    return b == null || a.isAfter(b) ? a : b;
  }

  /**
   * @param a première date (peut être null)
   * @param b seconde date (peut être null)
   * @return la plus ancienne des dates non nulles
   */
  public static LocalDate min(LocalDate a, LocalDate b) {
    if (a == null) {
      return b;
    }
    return b == null || a.isBefore(b) ? a : b;
  }

  /**
   * @param instant instant (peut être null)
   * @return le jour UTC correspondant
   */
  public static LocalDate jour(Instant instant) {
    return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
  }

  /**
   * @param a valeur prioritaire
   * @param b valeur de repli
   * @param <T> type
   * @return {@code a} s'il n'est pas nul, sinon {@code b}
   */
  public static <T> T sinon(T a, T b) {
    return a != null ? a : b;
  }
}
