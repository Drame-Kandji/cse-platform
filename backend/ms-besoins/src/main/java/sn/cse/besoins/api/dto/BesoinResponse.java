package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

/**
 * Ressource {@code Besoin} (contrat d'API §3).
 *
 * @param id identifiant
 * @param reference référence
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin
 * @param quantite quantité
 * @param specifications spécifications
 * @param priorite priorité
 * @param statut statut
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param demandeurId demandeur
 * @param demandeurNom nom du demandeur
 * @param dureeUtilisationJours durée
 * @param dateBesoin date de besoin
 * @param justification justification
 * @param motifRejet motif de rejet
 * @param validateurId auteur de la décision
 * @param validateurNom nom de l'auteur de la décision
 * @param dateValidation instant de la décision
 * @param materielIdsAffectes matériels affectés
 * @param orientationEnAttente orientation à relancer
 * @param createdAt soumission
 * @param updatedAt dernière modification
 * @param version version
 */
@Schema(name = "Besoin", description = "Besoin en matériel exprimé par un chantier")
public record BesoinResponse(
    @Schema(example = "3f2b8c1e-5d4a-4e8b-9c7d-1a2b3c4d5e6f") UUID id,
    @Schema(description = "Référence lisible", example = "BES-2026-0001") String reference,
    @Schema(example = "Pelle hydraulique 20 t") String designation,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "e0000000-0000-0000-0000-000000000001", nullable = true) UUID typeEnginId,
    @Schema(example = "2") int quantite,
    @Schema(example = "Godet 1 m³, chenilles", nullable = true) String specifications,
    @Schema(example = "URGENT") Priorite priorite,
    @Schema(example = "EN_ATTENTE") StatutBesoin statut,
    @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID chantierId,
    @Schema(example = "Extension autoroute Dakar-AIBD") String chantierNom,
    @Schema(example = "00000000-0000-0000-0000-000000000101") UUID demandeurId,
    @Schema(example = "Mamadou Diop") String demandeurNom,
    @Schema(example = "45") int dureeUtilisationJours,
    @Schema(example = "2026-11-15") LocalDate dateBesoin,
    @Schema(example = "Terrassement du lot 3 bloqué", nullable = true) String justification,
    @Schema(example = "Doublon d'un besoin existant", nullable = true) String motifRejet,
    @Schema(example = "00000000-0000-0000-0000-000000000201", nullable = true) UUID validateurId,
    @Schema(example = "Ibrahima Fall", nullable = true) String validateurNom,
    @Schema(example = "2026-10-30T09:15:00Z", nullable = true) Instant dateValidation,
    @ArraySchema(
            arraySchema = @Schema(description = "Matériels du parc affectés au besoin"),
            schema = @Schema(example = "a1000000-0000-0000-0000-000000000001"))
        List<UUID> materielIdsAffectes,
    @Schema(
            description =
                "Vrai si le besoin est VALIDE mais pas encore orienté (ms-suivi indisponible) :"
                    + " relancer via POST /api/besoins/{id}/orienter",
            example = "false")
        boolean orientationEnAttente,
    @Schema(example = "2026-10-28T08:00:00Z") Instant createdAt,
    @Schema(example = "2026-10-30T09:15:00Z") Instant updatedAt,
    @Schema(description = "Version (verrou optimiste)", example = "0") long version) {

  /**
   * @param v vue applicative
   * @return la ressource
   */
  public static BesoinResponse depuis(BesoinVue v) {
    return new BesoinResponse(
        v.id(),
        v.reference(),
        v.designation(),
        v.categorie(),
        v.typeEnginId(),
        v.quantite(),
        v.specifications(),
        v.priorite(),
        v.statut(),
        v.chantierId(),
        v.chantierNom(),
        v.demandeurId(),
        v.demandeurNom(),
        v.dureeUtilisationJours(),
        v.dateBesoin(),
        v.justification(),
        v.motifRejet(),
        v.validateurId(),
        v.validateurNom(),
        v.dateValidation(),
        v.materielIdsAffectes(),
        v.orientationEnAttente(),
        v.createdAt(),
        v.updatedAt(),
        v.version());
  }
}
