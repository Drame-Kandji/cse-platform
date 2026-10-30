package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.GraviteLitige;
import sn.cse.fournisseurs.domain.model.StatutLitige;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;

/**
 * Litige.
 *
 * @param id identifiant
 * @param reference référence
 * @param fournisseurId fournisseur
 * @param raisonSociale raison sociale
 * @param contratId contrat
 * @param objet objet
 * @param description description
 * @param gravite gravité
 * @param statut statut
 * @param dateOuverture ouverture
 * @param dateResolution résolution
 * @param actions chronologie
 */
@Schema(description = "Litige fournisseur")
public record LitigeDto(
    @Schema(example = "b0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "LIT-2026-001") String reference,
    @Schema(example = "f0000000-0000-0000-0000-000000000002") UUID fournisseurId,
    @Schema(example = "Teranga Équipements BTP") String raisonSociale,
    @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001") UUID contratId,
    @Schema(example = "Livraison incomplète de camions-bennes") String objet,
    @Schema(example = "2 camions sur 5 livrés au chantier CH-THS-01 à la date prévue")
        String description,
    @Schema(example = "MOYENNE") GraviteLitige gravite,
    @Schema(example = "EN_COURS") StatutLitige statut,
    @Schema(example = "2026-09-02T10:00:00Z") Instant dateOuverture,
    @Schema(example = "2026-09-20T16:00:00Z") Instant dateResolution,
    @Schema(description = "Chronologie des actions") List<Action> actions) {

  /**
   * Entrée de chronologie.
   *
   * @param date date
   * @param auteurNom auteur
   * @param type type
   * @param commentaire commentaire
   */
  @Schema(name = "ActionLitige", description = "Entrée de la chronologie d'un litige")
  public record Action(
      @Schema(example = "2026-09-05T11:00:00Z") Instant date,
      @Schema(example = "Fatou Sarr") String auteurNom,
      @Schema(example = "ACTION") TypeActionLitige type,
      @Schema(example = "Relance écrite adressée au fournisseur") String commentaire) {}
}
