package sn.cse.chatbot.domain.service;

import java.util.regex.Pattern;

/**
 * Protection contre l'injection de prompt : suppression des caractères de contrôle, neutralisation
 * des délimiteurs utilisés pour encadrer données et question, et bornage de la longueur.
 */
public final class AssainisseurTexte {

  /** Délimiteur ouvrant des blocs transmis au LLM. */
  public static final String OUVRANT = "<<<";

  /** Délimiteur fermant des blocs transmis au LLM. */
  public static final String FERMANT = ">>>";

  // Contrôles C0/C1 sauf tabulation et saut de ligne, caractères de format invisibles (zero-width,
  // marques bidirectionnelles) souvent utilisés pour masquer des instructions.
  private static final Pattern CONTROLES =
      Pattern.compile(
          "[\\p{Cc}&&[^\\n\\t]]|[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]");
  private static final Pattern CHEVRONS = Pattern.compile("<{3,}|>{3,}");

  private AssainisseurTexte() {}

  /**
   * @param texte texte brut (question ou donnée)
   * @return le texte sans caractères de contrôle ni délimiteurs, espaces normalisés
   */
  public static String nettoyer(String texte) {
    if (texte == null) {
      return "";
    }
    String t = CONTROLES.matcher(texte).replaceAll("");
    t = CHEVRONS.matcher(t).replaceAll("");
    return t.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
  }

  /**
   * @param texte texte
   * @param max longueur maximale
   * @return le texte tronqué (marqué « … ») si nécessaire
   */
  public static String tronquer(String texte, int max) {
    if (texte.length() <= max) {
      return texte;
    }
    return texte.substring(0, Math.max(0, max - 1)) + "…";
  }
}
