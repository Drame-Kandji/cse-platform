package sn.cse.chatbot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.chatbot.domain.model.TypeSource;

/**
 * Source citée.
 *
 * @param type type d'entité
 * @param id identifiant
 * @param libelle libellé
 * @param lien route frontend
 */
@Schema(name = "Source")
public record SourceDto(
    @Schema(example = "MATERIEL") TypeSource type,
    @Schema(example = "a1b2c3d4-0000-0000-0000-000000000010") String id,
    @Schema(example = "NIV-002 — Niveleuse CAT 140K") String libelle,
    @Schema(example = "/materiels/a1b2c3d4-0000-0000-0000-000000000010") String lien) {

  /**
   * @param s source du domaine
   * @return le DTO
   */
  public static SourceDto depuis(Source s) {
    return new SourceDto(s.type(), s.id(), s.libelle(), s.lien());
  }
}
