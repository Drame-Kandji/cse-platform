package sn.cse.besoins.domain.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalisation des désignations pour regrouper des demandes équivalentes : « Pelle hydraulique 20
 * T » et « pelle HYDRAULIQUE 20t » désignent le même matériel.
 */
public final class NormalisationDesignation {

  private static final Pattern DIACRITIQUES = Pattern.compile("\\p{M}+");
  private static final Pattern NON_ALPHANUMERIQUE = Pattern.compile("[^a-z0-9]+");
  private static final Pattern NOMBRE_PUIS_LETTRES = Pattern.compile("(\\d)\\s+([a-z])");

  private NormalisationDesignation() {}

  /**
   * @param designation désignation saisie
   * @return clé normalisée (minuscules, sans accents ni ponctuation, espaces réduits)
   */
  public static String normaliser(String designation) {
    if (designation == null) {
      return "";
    }
    String sansAccents =
        DIACRITIQUES.matcher(Normalizer.normalize(designation, Normalizer.Form.NFD)).replaceAll("");
    String simplifie =
        NON_ALPHANUMERIQUE.matcher(sansAccents.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    // « 20 t » et « 20t » sont équivalents.
    return NOMBRE_PUIS_LETTRES.matcher(simplifie).replaceAll("$1$2");
  }
}
