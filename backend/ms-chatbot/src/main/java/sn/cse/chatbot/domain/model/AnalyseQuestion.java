package sn.cse.chatbot.domain.model;

import java.util.Objects;

/**
 * Résultat de l'analyse d'intention.
 *
 * @param intention intention retenue
 * @param entites entités extraites
 * @param texteNormalise question normalisée (minuscules, sans accents)
 */
public record AnalyseQuestion(Intention intention, Entites entites, String texteNormalise) {

  public AnalyseQuestion {
    Objects.requireNonNull(intention, "intention");
    Objects.requireNonNull(entites, "entites");
  }
}
