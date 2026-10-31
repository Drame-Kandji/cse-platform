package sn.cse.reporting.infrastructure.export;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mise en forme française des valeurs exportées. Le séparateur de milliers est l'espace insécable
 * U+00A0 (et non l'espace fine U+202F du CLDR, absente des polices PDF standard WinAnsi).
 */
final class FormatFr {

  /** Espace insécable. */
  static final char ESPACE = ' ';

  /** Texte affiché pour une valeur non définie. */
  static final String NON_DEFINI = "-";

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
  private static final DateTimeFormatter HEURE =
      DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE);

  private FormatFr() {}

  private static DecimalFormat format(String motif) {
    DecimalFormatSymbols s = DecimalFormatSymbols.getInstance(Locale.FRANCE);
    s.setGroupingSeparator(ESPACE);
    s.setDecimalSeparator(',');
    return new DecimalFormat(motif, s);
  }

  /**
   * @param v montant
   * @return {@code 12 500 000 FCFA}
   */
  static String montant(BigDecimal v) {
    return v == null ? NON_DEFINI : format("#,##0").format(v) + ESPACE + "FCFA";
  }

  /**
   * @param v pourcentage 0..100
   * @return {@code 85,3 %}
   */
  static String taux(BigDecimal v) {
    return v == null ? NON_DEFINI : format("#,##0.0").format(v) + ESPACE + "%";
  }

  /**
   * @param v nombre entier
   * @return {@code 1 234}
   */
  static String nombre(BigDecimal v) {
    return v == null ? NON_DEFINI : format("#,##0").format(v);
  }

  /**
   * @param v score décimal
   * @return {@code 85,5}
   */
  static String score(BigDecimal v) {
    return v == null ? NON_DEFINI : format("#,##0.0").format(v);
  }

  /**
   * @param v nombre décimal
   * @param unite unité affichée
   * @return {@code 12,5 j}
   */
  static String decimal(BigDecimal v, String unite) {
    return v == null ? NON_DEFINI : format("#,##0.0").format(v) + ESPACE + unite;
  }

  /**
   * @param d date
   * @return {@code 30/10/2026}
   */
  static String date(LocalDate d) {
    return d == null ? NON_DEFINI : DATE.format(d);
  }

  /**
   * @param i instant
   * @return {@code 30/10/2026 à 09:15 (UTC)}
   */
  static String horodatage(Instant i) {
    return DATE.format(i.atZone(ZoneOffset.UTC))
        + " à "
        + HEURE.format(i.atZone(ZoneOffset.UTC))
        + " (UTC)";
  }
}
