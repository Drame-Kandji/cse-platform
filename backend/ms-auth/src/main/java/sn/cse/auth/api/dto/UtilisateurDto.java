package sn.cse.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.auth.domain.model.Role;

/**
 * Profil utilisateur exposé par l'API (contrat §9).
 *
 * @param id identifiant Keycloak ({@code sub})
 * @param email adresse e-mail
 * @param prenom prénom
 * @param nom nom
 * @param telephone téléphone
 * @param role rôle unique
 * @param actif compte actif
 * @param chantierIds chantiers attribués (chef de chantier)
 * @param perimetreParc périmètre du parc (responsable matériel)
 * @param seuilDelegation seuil de délégation en FCFA (responsable des achats)
 * @param budgetGere budget géré en FCFA (DAF)
 * @param fournisseurId fiche fournisseur rattachée (fournisseur)
 * @param otpConfigure second facteur TOTP configuré
 * @param createdAt date de création du profil
 */
@Schema(name = "Utilisateur", description = "Profil d'un utilisateur de la plateforme")
public record UtilisateurDto(
    @Schema(
            description = "Identifiant Keycloak (sub)",
            example = "00000000-0000-0000-0000-000000000101")
        UUID id,
    @Schema(
            description = "Adresse e-mail (identifiant de connexion)",
            example = "chef.chantier@cse.sn")
        String email,
    @Schema(description = "Prénom", example = "Mamadou") String prenom,
    @Schema(description = "Nom", example = "Diop") String nom,
    @Schema(description = "Téléphone", example = "+221 77 123 45 67", nullable = true)
        String telephone,
    @Schema(description = "Rôle unique de l'utilisateur", example = "CHEF_CHANTIER") Role role,
    @Schema(description = "Compte actif (connexion autorisée)", example = "true") boolean actif,
    @Schema(
            description = "Chantiers attribués (chef de chantier uniquement)",
            example = "[\"c0000000-0000-0000-0000-000000000001\"]",
            nullable = true)
        List<UUID> chantierIds,
    @Schema(
            description = "Périmètre du parc (responsable matériel)",
            example = "NATIONAL",
            nullable = true)
        String perimetreParc,
    @Schema(
            description = "Seuil de délégation en FCFA (responsable des achats)",
            example = "150000000",
            nullable = true)
        BigDecimal seuilDelegation,
    @Schema(description = "Budget géré en FCFA (DAF)", example = "5000000000", nullable = true)
        BigDecimal budgetGere,
    @Schema(
            description = "Fiche fournisseur rattachée (compte fournisseur)",
            example = "f0000000-0000-0000-0000-000000000001",
            nullable = true)
        UUID fournisseurId,
    @Schema(description = "Second facteur TOTP configuré dans Keycloak", example = "false")
        boolean otpConfigure,
    @Schema(description = "Date de création du profil", example = "2026-10-30T09:15:00Z")
        Instant createdAt) {}
