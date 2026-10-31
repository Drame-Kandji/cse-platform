package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.achats.domain.model.StatutFacture;

/**
 * Facture fournisseur.
 *
 * @param id identifiant
 * @param numero numéro
 * @param contratId contrat
 * @param contratNumero numéro du contrat
 * @param fournisseurId fournisseur
 * @param fournisseurNom fournisseur
 * @param montantHt hors taxes
 * @param tva TVA
 * @param montantTtc TTC
 * @param dateEmission émission
 * @param dateEcheance échéance
 * @param statut statut
 * @param referenceMandat mandat
 * @param dateMandatement mandatement
 * @param datePaiement paiement
 * @param motifRejet motif de rejet
 * @param enRetard vrai si non payée après échéance
 */
@Schema(name = "Facture", description = "Facture fournisseur")
public record FactureDto(
    @Schema(example = "9d7e1b52-3f0c-4a8e-9d55-0c1b2a3d4e5f") UUID id,
    @Schema(example = "FA-SE-2026-118") String numero,
    @Schema(example = "a0000000-0000-0000-0000-000000000001") UUID contratId,
    @Schema(example = "CTR-2026-001") String contratNumero,
    @Schema(example = "f0000000-0000-0000-0000-000000000001") UUID fournisseurId,
    @Schema(example = "Sahel Engins SA") String fournisseurNom,
    @Schema(example = "120000000") BigDecimal montantHt,
    @Schema(example = "21600000") BigDecimal tva,
    @Schema(example = "141600000") BigDecimal montantTtc,
    @Schema(example = "2026-10-20") LocalDate dateEmission,
    @Schema(example = "2026-11-19") LocalDate dateEcheance,
    @Schema(example = "RECUE") StatutFacture statut,
    @Schema(example = "MDT-2026-0087") String referenceMandat,
    @Schema(example = "2026-10-25") LocalDate dateMandatement,
    @Schema(example = "2026-11-05") LocalDate datePaiement,
    @Schema(example = "Quantités facturées non conformes au bon de livraison") String motifRejet,
    @Schema(example = "false") boolean enRetard) {}
