package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Déclaration d'expédition par le fournisseur.
 *
 * @param dateExpedition date d'expédition (pas dans le futur)
 * @param commentaire commentaire facultatif (transporteur, suivi)
 */
@Schema(name = "ExpedierRequest", description = "Déclaration d'expédition du matériel")
public record ExpedierRequest(
    @Schema(example = "2026-11-02") @NotNull LocalDate dateExpedition,
    @Schema(example = "Départ du port de Dakar par porte-char, arrivée prévue sous 48 h")
        @Size(max = 2000)
        String commentaire) {}
