package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.StatutChantier;

/**
 * Chantier.
 *
 * @param id identifiant
 * @param code code
 * @param nom nom
 * @param localisation localisation
 * @param region région
 * @param latitude latitude
 * @param longitude longitude
 * @param statut statut
 * @param budget budget (FCFA)
 * @param montantMarche montant du marché (FCFA)
 * @param dateDebut date de début
 * @param dateFinPrevue fin prévue
 * @param chefChantierId chef de chantier
 * @param chefChantierNom nom du chef
 * @param nombreMateriels matériels présents
 * @param version version (verrou optimiste)
 */
@Schema(name = "Chantier", description = "Chantier de la CSE")
public record ChantierDto(
    @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "CH-DKR-01") String code,
    @Schema(example = "Extension autoroute Dakar–AIBD") String nom,
    @Schema(example = "Diamniadio, Dakar") String localisation,
    @Schema(example = "Dakar") String region,
    @Schema(example = "14.7167") double latitude,
    @Schema(example = "-17.4677") double longitude,
    @Schema(example = "EN_COURS") StatutChantier statut,
    @Schema(description = "Budget en FCFA", example = "18500000000") BigDecimal budget,
    @Schema(description = "Montant du marché en FCFA", example = "21000000000")
        BigDecimal montantMarche,
    @Schema(example = "2026-02-01") LocalDate dateDebut,
    @Schema(example = "2027-12-31", nullable = true) LocalDate dateFinPrevue,
    @Schema(example = "00000000-0000-0000-0000-000000000101", nullable = true) UUID chefChantierId,
    @Schema(example = "Mamadou Diop", nullable = true) String chefChantierNom,
    @Schema(description = "Matériels présents sur le chantier", example = "8") long nombreMateriels,
    @Schema(example = "3") Long version) {}
