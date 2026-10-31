package sn.cse.chatbot.domain.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Libellés français des codes métier et mises en forme (dates, montants). */
public final class Libelles {

  private static final Locale FR = Locale.FRANCE;
  private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);
  private static final DateTimeFormatter HEURE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm", FR).withZone(ZoneOffset.UTC);

  private static final Map<String, String> CODES =
      Map.ofEntries(
          Map.entry("MATERIEL_ROULANT", "matériel roulant"),
          Map.entry("OUTILLAGE", "outillage"),
          Map.entry("EQUIPEMENT_INFORMATIQUE", "équipement informatique"),
          Map.entry("CONSOMMABLE", "consommable"),
          Map.entry("DISPONIBLE", "disponible"),
          Map.entry("AFFECTE", "affecté"),
          Map.entry("EN_MAINTENANCE", "en maintenance"),
          Map.entry("REFORME", "réformé"),
          Map.entry("EN_ATTENTE", "en attente de validation"),
          Map.entry("VALIDE", "validé"),
          Map.entry("REJETE", "rejeté"),
          Map.entry("AFFECTE_DIRECT", "satisfait par affectation directe"),
          Map.entry("EN_ACHAT", "transmis aux achats"),
          Map.entry("NORMAL", "normale"),
          Map.entry("URGENT", "urgente"),
          Map.entry("CRITIQUE", "critique"),
          Map.entry("EN_ATTENTE_RECEPTION", "en attente de réception"),
          Map.entry("EN_COURS", "en cours"),
          Map.entry("TERMINEE", "terminée"),
          Map.entry("PREVENTIVE", "préventive"),
          Map.entry("CORRECTIVE", "corrective"),
          Map.entry("LOURDE", "lourde"),
          Map.entry("PLANIFIEE", "planifiée"),
          Map.entry("CLOTUREE", "clôturée"),
          Map.entry("BROUILLON", "brouillon"),
          Map.entry("SIGNE", "signé, engagement en attente"),
          Map.entry("ENGAGEMENT_VALIDE", "engagement validé"),
          Map.entry("EN_LIVRAISON", "en cours de livraison"),
          Map.entry("LIVRE", "livré"),
          Map.entry("CLOTURE", "clôturé"),
          Map.entry("PUBLIE", "publié (offres en cours de réception)"),
          Map.entry("ATTRIBUE", "attribué"),
          Map.entry("TERMINE", "terminé"));

  private Libelles() {}

  /**
   * @param code code d'énumération
   * @return le libellé français (le code lui-même s'il est inconnu)
   */
  public static String de(String code) {
    return code == null ? "-" : CODES.getOrDefault(code, code.toLowerCase(FR).replace('_', ' '));
  }

  /**
   * @param date date
   * @return {@code jj/mm/aaaa} ou « - »
   */
  public static String date(LocalDate date) {
    return date == null ? "-" : date.format(JOUR);
  }

  /**
   * @param instant instant
   * @return {@code jj/mm/aaaa à hh:mm} (UTC) ou « - »
   */
  public static String date(Instant instant) {
    return instant == null ? "-" : HEURE.format(instant);
  }

  /**
   * @param montant montant
   * @return montant groupé suivi de « FCFA »
   */
  public static String montant(BigDecimal montant) {
    NumberFormat f = NumberFormat.getIntegerInstance(FR);
    return f.format(montant).replace(' ', ' ').replace(' ', ' ') + " FCFA";
  }

  /**
   * @param valeur nombre décimal
   * @return valeur à une décimale au plus
   */
  public static String nombre(BigDecimal valeur) {
    NumberFormat f = NumberFormat.getNumberInstance(FR);
    f.setMaximumFractionDigits(1);
    return f.format(valeur);
  }
}
