package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.service.LigneConsolidation;

/**
 * Ressource {@code Consolidation}.
 *
 * @param categorie catégorie
 * @param designation désignation représentative
 * @param quantiteTotale quantité cumulée
 * @param nombreDemandes nombre de besoins
 * @param prioriteMax priorité maximale
 * @param chantiers chantiers concernés
 * @param besoinIds besoins regroupés
 * @param doublonPotentiel doublon potentiel détecté
 */
@Schema(name = "Consolidation", description = "Regroupement de besoins équivalents")
public record ConsolidationResponse(
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "Pelle hydraulique 20 t") String designation,
    @Schema(example = "5") int quantiteTotale,
    @Schema(example = "3") int nombreDemandes,
    @Schema(example = "CRITIQUE") Priorite prioriteMax,
    @ArraySchema(arraySchema = @Schema(description = "Chantiers concernés"))
        List<ChantierResume> chantiers,
    @ArraySchema(
            arraySchema = @Schema(description = "Besoins regroupés"),
            schema = @Schema(example = "3f2b8c1e-5d4a-4e8b-9c7d-1a2b3c4d5e6f"))
        List<UUID> besoinIds,
    @Schema(
            description =
                "Vrai si un même chantier a exprimé ce besoin plusieurs fois dans la fenêtre"
                    + " configurée",
            example = "false")
        boolean doublonPotentiel) {

  /**
   * Chantier concerné.
   *
   * @param id identifiant
   * @param nom nom
   */
  @Schema(name = "ChantierResume", description = "Chantier concerné par un regroupement")
  public record ChantierResume(
      @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID id,
      @Schema(example = "Extension autoroute Dakar-AIBD") String nom) {}

  /**
   * @param l ligne du domaine
   * @return la ressource
   */
  public static ConsolidationResponse depuis(LigneConsolidation l) {
    return new ConsolidationResponse(
        l.categorie(),
        l.designation(),
        l.quantiteTotale(),
        l.nombreDemandes(),
        l.prioriteMax(),
        l.chantiers().stream().map(c -> new ChantierResume(c.id(), c.nom())).toList(),
        l.besoinIds(),
        l.doublonPotentiel());
  }
}
