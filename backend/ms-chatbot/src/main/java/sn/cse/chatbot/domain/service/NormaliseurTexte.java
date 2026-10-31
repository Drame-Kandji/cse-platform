package sn.cse.chatbot.domain.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Normalisation insensible aux accents et à la casse, utilisée par la classification. */
public final class NormaliseurTexte {

  private static final Pattern DIACRITIQUES = Pattern.compile("\\p{M}+");
  private static final Pattern SEPARATEURS = Pattern.compile("[^a-z0-9'\\-]+");

  private NormaliseurTexte() {}

  /**
   * « Où se trouve l’Engin ? » → « ou se trouve l'engin ».
   *
   * @param texte texte libre
   * @return texte en minuscules, sans accents ni ponctuation (apostrophes et tirets conservés)
   */
  public static String normaliser(String texte) {
    if (texte == null) {
      return "";
    }
    String sansAccents =
        DIACRITIQUES
            .matcher(Normalizer.normalize(texte, Normalizer.Form.NFD))
            .replaceAll("")
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('œ', 'o')
            .toLowerCase(Locale.ROOT);
    return SEPARATEURS.matcher(sansAccents).replaceAll(" ").trim();
  }
}
