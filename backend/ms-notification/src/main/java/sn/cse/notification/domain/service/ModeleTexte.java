package sn.cse.notification.domain.service;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Résolution des modèles de texte à marqueurs {@code {champ}} (ex. {@code "Besoin {reference}
 * validé"}). Les marqueurs inconnus sont remplacés par une valeur de repli et signalés, ce qui
 * permet d'abandonner un lien incomplet plutôt que de produire une route invalide.
 */
public final class ModeleTexte {

  private static final Pattern MARQUEUR = Pattern.compile("\\{([A-Za-z0-9_.]+)}");

  private ModeleTexte() {}

  /**
   * Résultat d'une résolution.
   *
   * @param texte texte produit
   * @param complet vrai si tous les marqueurs ont été trouvés
   */
  public record Resolution(String texte, boolean complet) {}

  /**
   * @param modele modèle (peut être {@code null})
   * @param valeurs accès aux valeurs déjà mises en forme
   * @param repli texte utilisé pour un marqueur introuvable
   * @return la résolution ({@code null} si le modèle est nul)
   */
  public static Resolution resoudre(
      String modele, Function<String, Optional<String>> valeurs, String repli) {
    if (modele == null) {
      return null;
    }
    Matcher m = MARQUEUR.matcher(modele);
    StringBuilder sb = new StringBuilder();
    boolean complet = true;
    while (m.find()) {
      Optional<String> valeur = valeurs.apply(m.group(1)).filter(v -> !v.isBlank());
      if (valeur.isEmpty()) {
        complet = false;
      }
      m.appendReplacement(sb, Matcher.quoteReplacement(valeur.orElse(repli)));
    }
    m.appendTail(sb);
    return new Resolution(sb.toString().replaceAll(" {2,}", " ").trim(), complet);
  }
}
