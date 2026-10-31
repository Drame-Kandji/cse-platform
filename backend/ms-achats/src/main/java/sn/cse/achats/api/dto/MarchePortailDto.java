package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.achats.domain.model.StatutContrat;

/**
 * Marché attribué au fournisseur connecté.
 *
 * @param contratId contrat
 * @param numero numéro
 * @param appelOffreReference référence de l'appel d'offres
 * @param objet objet
 * @param montant montant
 * @param statut statut
 * @param dateSignature signature
 * @param dateLivraisonPrevue échéance
 * @param dateLivraisonReelle livraison
 */
@Schema(name = "MarchePortail", description = "Marché attribué au fournisseur connecté")
public record MarchePortailDto(
    @Schema(example = "a0000000-0000-0000-0000-000000000001") UUID contratId,
    @Schema(example = "CTR-2026-001") String numero,
    @Schema(example = "AO-2026-001") String appelOffreReference,
    @Schema(example = "Acquisition de deux pelles hydrauliques 20 t") String objet,
    @Schema(example = "285000000") BigDecimal montant,
    @Schema(example = "EN_LIVRAISON") StatutContrat statut,
    @Schema(example = "2026-10-15") LocalDate dateSignature,
    @Schema(example = "2026-11-29") LocalDate dateLivraisonPrevue,
    @Schema(example = "2026-12-02") LocalDate dateLivraisonReelle) {}
