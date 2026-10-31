package sn.cse.chatbot.domain.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import sn.cse.chatbot.domain.model.Procedure;

/** Recherche des procédures pertinentes par mots-clés (insensible aux accents). */
public class BaseProcedures {

  private final List<Procedure> procedures;
  private final int maximum;

  /**
   * @param procedures procédures configurées
   * @param maximum nombre maximal de procédures retournées
   */
  public BaseProcedures(List<Procedure> procedures, int maximum) {
    this.procedures = List.copyOf(procedures);
    this.maximum = maximum;
  }

  /**
   * @param texteNormalise question normalisée
   * @return les procédures les plus pertinentes (aucune si rien ne correspond)
   */
  public List<Procedure> rechercher(String texteNormalise) {
    return procedures.stream()
        .map(p -> Map.entry(p, score(p, texteNormalise)))
        .filter(e -> e.getValue() > 0)
        .sorted(Map.Entry.<Procedure, Integer>comparingByValue(Comparator.reverseOrder()))
        .limit(maximum)
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * @return toutes les procédures (sommaire)
   */
  public List<Procedure> toutes() {
    return procedures;
  }

  private static int score(Procedure p, String texte) {
    int score = 0;
    for (String mot : p.motsCles()) {
      String n = NormaliseurTexte.normaliser(mot);
      if (!n.isEmpty()
          && Pattern.compile("(?<![a-z0-9])" + Pattern.quote(n)).matcher(texte).find()) {
        score += n.contains(" ") ? 2 : 1;
      }
    }
    return score;
  }
}
