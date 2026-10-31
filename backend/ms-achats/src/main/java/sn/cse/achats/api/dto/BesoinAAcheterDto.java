package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.Priorite;

/**
 * Besoin transmis aux achats.
 *
 * @param besoinId besoin
 * @param reference référence
 * @param designation désignation
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param priorite priorité
 * @param chantierId chantier
 * @param chantierNom chantier
 * @param dateReception réception
 * @param appelOffreId appel d'offres lancé
 */
@Schema(
    name = "BesoinAAcheter",
    description = "Besoin orienté vers l'achat (projection de BESOIN_EN_ACHAT)")
public record BesoinAAcheterDto(
    @Schema(example = "b0000000-0000-0000-0000-000000000001") UUID besoinId,
    @Schema(example = "BES-2026-0001") String reference,
    @Schema(example = "Pelle hydraulique 20 t") String designation,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "2") int quantite,
    @Schema(example = "Godet 1,2 m3") String specifications,
    @Schema(example = "URGENT") Priorite priorite,
    @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID chantierId,
    @Schema(example = "Extension autoroute Dakar–AIBD") String chantierNom,
    @Schema(example = "2026-10-30T09:15:00Z") Instant dateReception,
    @Schema(example = "d0000000-0000-0000-0000-000000000001") UUID appelOffreId) {}
