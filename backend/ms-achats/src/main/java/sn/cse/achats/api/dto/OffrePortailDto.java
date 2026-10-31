package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import sn.cse.achats.domain.model.StatutAO;

/**
 * Offre du fournisseur connecté.
 *
 * @param id identifiant
 * @param appelOffreId appel d'offres
 * @param appelOffreReference référence
 * @param appelOffreObjet objet
 * @param statutAppelOffre statut de l'appel d'offres
 * @param dateLimite date limite
 * @param codeAnonyme code de l'offre
 * @param montantPropose montant
 * @param delaiLivraisonJours délai
 * @param garantieMois garantie
 * @param description description
 * @param dateDepot dépôt
 * @param retenue offre retenue
 */
@Schema(name = "OffrePortail", description = "Offre déposée par le fournisseur connecté")
public record OffrePortailDto(
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") UUID id,
    @Schema(example = "d0000000-0000-0000-0000-000000000012") UUID appelOffreId,
    @Schema(example = "AO-2026-012") String appelOffreReference,
    @Schema(example = "Fourniture de 3 camions-bennes 20 m3") String appelOffreObjet,
    @Schema(example = "PUBLIE") StatutAO statutAppelOffre,
    @Schema(example = "2026-11-09T17:00:00Z") Instant dateLimite,
    @Schema(example = "OFR-7F3A2C") String codeAnonyme,
    @Schema(example = "96000000") BigDecimal montantPropose,
    @Schema(example = "30") int delaiLivraisonJours,
    @Schema(example = "12") Integer garantieMois,
    @Schema(example = "Camions Mercedes Actros neufs") String description,
    @Schema(example = "2026-10-25T10:00:00Z") Instant dateDepot,
    @Schema(example = "false") boolean retenue) {}
