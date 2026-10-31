package sn.cse.chatbot.domain.model;

import java.util.Objects;

/**
 * Donnée citée à l'appui d'une réponse, avec un lien vers l'écran correspondant.
 *
 * @param type type d'entité
 * @param id identifiant
 * @param libelle libellé affichable
 * @param lien route frontend
 */
public record Source(TypeSource type, String id, String libelle, String lien) {

  public Source {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(id, "id");
  }
}
