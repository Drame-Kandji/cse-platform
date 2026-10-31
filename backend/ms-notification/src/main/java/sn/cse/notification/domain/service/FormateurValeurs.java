package sn.cse.notification.domain.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mise en forme française des valeurs injectées dans les modèles : nombres groupés ({@code 150 000
 * 000}), dates {@code jj/mm/aaaa}, codes d'énumération traduits par un dictionnaire de libellés
 * configurable ({@code BLOQUANTE → bloquante}).
 */
public class FormateurValeurs {

  private static final Locale FR = Locale.FRANCE;
  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final Pattern INSTANT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T.*Z");
  private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);
  private static final DateTimeFormatter HORODATAGE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", FR).withZone(ZoneOffset.UTC);

  private final Map<String, String> libelles;

  /**
   * @param libelles traduction des codes (clé = code, valeur = libellé)
   */
  public FormateurValeurs(Map<String, String> libelles) {
    this.libelles = libelles == null ? Map.of() : Map.copyOf(libelles);
  }

  /**
   * @param valeur valeur brute du payload
   * @return la valeur mise en forme
   */
  public String formater(Object valeur) {
    if (valeur == null) {
      return "";
    }
    if (valeur instanceof Number n) {
      return nombre(new BigDecimal(n.toString()));
    }
    if (valeur instanceof Collection<?> c) {
      return c.stream().map(this::formater).collect(Collectors.joining(", "));
    }
    String texte = String.valueOf(valeur);
    if (DATE.matcher(texte).matches()) {
      return date(texte);
    }
    if (INSTANT.matcher(texte).matches()) {
      return horodatage(texte);
    }
    return libelles.getOrDefault(texte, texte);
  }

  private static String nombre(BigDecimal n) {
    NumberFormat format = NumberFormat.getNumberInstance(FR);
    format.setMaximumFractionDigits(1);
    format.setGroupingUsed(true);
    // Espace insécable classique plutôt que l'espace fine (rendu homogène dans les e-mails).
    return format.format(n).replace(' ', ' ');
  }

  private static String date(String texte) {
    try {
      return LocalDate.parse(texte).format(JOUR);
    } catch (DateTimeParseException e) {
      return texte;
    }
  }

  private static String horodatage(String texte) {
    try {
      return HORODATAGE.format(Instant.parse(texte));
    } catch (DateTimeParseException e) {
      return texte;
    }
  }
}
