package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/**
 * Accusé de réception d'une inscription (contrat §13).
 *
 * @param id identifiant du fournisseur
 * @param raisonSociale raison sociale
 * @param statut statut ({@code EN_ATTENTE_VALIDATION})
 */
@Schema(description = "Inscription enregistrée, en attente de validation par le service des achats")
public record InscriptionDto(
    @Schema(description = "Identifiant", example = "f0000000-0000-0000-0000-000000000007") UUID id,
    @Schema(description = "Raison sociale", example = "Baobab Logistique SARL")
        String raisonSociale,
    @Schema(description = "Statut", example = "EN_ATTENTE_VALIDATION") StatutFournisseur statut) {}
