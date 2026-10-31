package sn.cse.chatbot.domain.model;

import java.util.List;

/**
 * Procédure documentée (base de connaissances configurable).
 *
 * @param sujet titre
 * @param motsCles mots-clés déclencheurs (normalisés à l'usage)
 * @param etapes étapes ordonnées
 */
public record Procedure(String sujet, List<String> motsCles, List<String> etapes) {

  public Procedure {
    motsCles = motsCles == null ? List.of() : List.copyOf(motsCles);
    etapes = etapes == null ? List.of() : List.copyOf(etapes);
  }
}
