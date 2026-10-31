package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.TypeAppelOffre;

/**
 * Appel d'offres.
 *
 * @param id identifiant
 * @param reference référence
 * @param objet objet
 * @param description description
 * @param type procédure
 * @param besoinId besoin d'origine
 * @param besoinReference référence du besoin
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param budgetEstime budget estimé
 * @param datePublication publication
 * @param dateLimite date limite de dépôt
 * @param statut statut
 * @param criteres critères pondérés
 * @param fournisseursInvites invités
 * @param nombreOffres nombre d'offres reçues
 * @param offreRetenueId offre retenue
 * @param montantAttribue montant attribué
 * @param attribution proposition d'attribution
 * @param creeParNom auteur
 * @param version version (verrou optimiste)
 */
@Schema(name = "AppelOffre", description = "Appel d'offres ou demande de prix")
public record AppelOffreDto(
    @Schema(example = "d0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "AO-2026-001") String reference,
    @Schema(example = "Acquisition de deux pelles hydrauliques 20 t") String objet,
    @Schema(example = "Pelles sur chenilles pour les terrassements de l'autoroute Dakar–AIBD")
        String description,
    @Schema(example = "APPEL_OFFRES") TypeAppelOffre type,
    @Schema(example = "b0000000-0000-0000-0000-000000000001") UUID besoinId,
    @Schema(example = "BES-2026-0001") String besoinReference,
    @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID chantierId,
    @Schema(example = "Extension autoroute Dakar–AIBD") String chantierNom,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "2") int quantite,
    @Schema(example = "Godet 1,2 m3, moteur Tier 3") String specifications,
    @Schema(description = "Budget estimé en FCFA", example = "300000000") BigDecimal budgetEstime,
    @Schema(example = "2026-10-01T08:00:00Z") Instant datePublication,
    @Schema(example = "2026-10-20T17:00:00Z") Instant dateLimite,
    @Schema(example = "PUBLIE") StatutAO statut,
    List<CriterePondereDto> criteres,
    List<FournisseurInviteDto> fournisseursInvites,
    @Schema(example = "3") int nombreOffres,
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") UUID offreRetenueId,
    @Schema(example = "285000000") BigDecimal montantAttribue,
    AttributionDto attribution,
    @Schema(example = "Fatou Sarr") String creeParNom,
    @Schema(example = "3") long version) {}
