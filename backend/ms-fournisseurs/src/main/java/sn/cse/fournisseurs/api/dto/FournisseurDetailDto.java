package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/**
 * Fournisseur détaillé : champs de {@link FournisseurDto} + documents + coordonnées bancaires (RA
 * et DAF uniquement, {@code null} pour les autres rôles).
 *
 * @param id identifiant
 * @param raisonSociale raison sociale
 * @param ninea NINEA
 * @param rccm RCCM
 * @param adresse adresse
 * @param ville ville
 * @param pays pays
 * @param telephone téléphone
 * @param email e-mail
 * @param contactNom interlocuteur
 * @param contactFonction fonction
 * @param specialites spécialités
 * @param certifications certifications
 * @param statut statut
 * @param motifStatut motif du statut
 * @param scorePerformance score 0..100
 * @param nombreEvaluations nombre d'évaluations
 * @param tauxLivraisonALHeure taux 0..100
 * @param nombreLitigesOuverts litiges ouverts
 * @param documentsExpires documents expirés
 * @param dateInscription date d'inscription
 * @param blacklistageDemande demande en attente
 * @param version version
 * @param documents documents administratifs
 * @param coordonneesBancaires coordonnées bancaires (RA et DAF seulement)
 */
@Schema(description = "Fournisseur détaillé")
public record FournisseurDetailDto(
    @Schema(example = "f0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "Sahel Engins SA") String raisonSociale,
    @Schema(example = "005123456 2G3") String ninea,
    @Schema(example = "SN-DKR-2015-B-12345") String rccm,
    @Schema(example = "Km 4,5 boulevard du Centenaire de la Commune") String adresse,
    @Schema(example = "Dakar") String ville,
    @Schema(example = "Sénégal") String pays,
    @Schema(example = "+221 33 849 12 34") String telephone,
    @Schema(example = "contact@sahel-engins.sn") String email,
    @Schema(example = "Abdoulaye Mbaye") String contactNom,
    @Schema(example = "Directeur commercial") String contactFonction,
    @Schema(example = "[\"MATERIEL_ROULANT\"]") List<CategorieMateriel> specialites,
    @Schema(example = "[\"ISO 9001:2015\"]") List<String> certifications,
    @Schema(example = "ACTIF") StatutFournisseur statut,
    @Schema(example = "Retards de livraison répétés") String motifStatut,
    @Schema(description = "Score de performance 0..100", example = "82.5")
        BigDecimal scorePerformance,
    @Schema(example = "6") int nombreEvaluations,
    @Schema(description = "Taux de livraison à l'heure 0..100", example = "87.5")
        BigDecimal tauxLivraisonALHeure,
    @Schema(example = "1") int nombreLitigesOuverts,
    @Schema(example = "0") int documentsExpires,
    @Schema(example = "2025-11-03") LocalDate dateInscription,
    @Schema(description = "Demande de blacklistage en attente (R09)")
        BlacklistageDemandeDto blacklistageDemande,
    @Schema(example = "4") long version,
    @Schema(description = "Documents administratifs") List<DocumentDto> documents,
    @Schema(description = "Coordonnées bancaires · RA et DAF uniquement, null sinon")
        CoordonneesBancairesDto coordonneesBancaires) {}
