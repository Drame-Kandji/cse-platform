package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.StatutContrat;

/**
 * Contrat d'achat.
 *
 * @param id identifiant
 * @param numero numéro
 * @param appelOffreId appel d'offres
 * @param appelOffreReference référence de l'appel d'offres
 * @param offreId offre retenue
 * @param fournisseurId titulaire
 * @param fournisseurNom titulaire
 * @param besoinId besoin
 * @param chantierId chantier
 * @param chantierNom chantier
 * @param objet objet
 * @param designation désignation
 * @param categorie catégorie
 * @param quantite quantité
 * @param montant montant
 * @param statut statut
 * @param dateSignature signature
 * @param delaiLivraisonJours délai
 * @param dateLivraisonPrevue échéance
 * @param dateLivraisonReelle livraison
 * @param numeroBonLivraison bon de livraison
 * @param clauses clauses
 * @param engagement engagement DAF
 * @param jalons jalons
 * @param penalitesRetard pénalités de retard
 * @param version version
 */
@Schema(name = "Contrat", description = "Contrat d'achat")
public record ContratDto(
    @Schema(example = "a0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "CTR-2026-001") String numero,
    @Schema(example = "d0000000-0000-0000-0000-000000000001") UUID appelOffreId,
    @Schema(example = "AO-2026-001") String appelOffreReference,
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") UUID offreId,
    @Schema(example = "f0000000-0000-0000-0000-000000000001") UUID fournisseurId,
    @Schema(example = "Sahel Engins SA") String fournisseurNom,
    @Schema(example = "b0000000-0000-0000-0000-000000000001") UUID besoinId,
    @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID chantierId,
    @Schema(example = "Extension autoroute Dakar–AIBD") String chantierNom,
    @Schema(example = "Acquisition de deux pelles hydrauliques 20 t") String objet,
    @Schema(example = "Pelle hydraulique 20 t") String designation,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "2") int quantite,
    @Schema(description = "Montant en FCFA", example = "285000000") BigDecimal montant,
    @Schema(example = "SIGNE") StatutContrat statut,
    @Schema(example = "2026-10-15") LocalDate dateSignature,
    @Schema(example = "45") int delaiLivraisonJours,
    @Schema(example = "2026-11-29") LocalDate dateLivraisonPrevue,
    @Schema(example = "2026-12-02") LocalDate dateLivraisonReelle,
    @Schema(example = "BL-SE-2026-0412") String numeroBonLivraison,
    ClausesDto clauses,
    EngagementContratDto engagement,
    List<JalonDto> jalons,
    @Schema(description = "Pénalités de retard en FCFA", example = "855000")
        BigDecimal penalitesRetard,
    @Schema(example = "4") long version) {}
