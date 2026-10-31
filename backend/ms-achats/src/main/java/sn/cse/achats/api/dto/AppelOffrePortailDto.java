package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.TypeAppelOffre;

/**
 * Appel d'offres vu depuis le portail (sans budget ni liste des autres invités).
 *
 * @param id identifiant
 * @param reference référence
 * @param objet objet
 * @param description description
 * @param type procédure
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param datePublication publication
 * @param dateLimite date limite
 * @param statut statut
 * @param criteres critères pondérés
 * @param offreDeposee vrai si le fournisseur connecté a déjà déposé une offre
 */
@Schema(name = "AppelOffrePortail", description = "Appel d'offres ouvert, vu par un fournisseur")
public record AppelOffrePortailDto(
    @Schema(example = "d0000000-0000-0000-0000-000000000012") UUID id,
    @Schema(example = "AO-2026-012") String reference,
    @Schema(example = "Fourniture de 3 camions-bennes 20 m3") String objet,
    @Schema(example = "Camions 6x4 pour le contournement de Thiès") String description,
    @Schema(example = "APPEL_OFFRES") TypeAppelOffre type,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "3") int quantite,
    @Schema(example = "Benne 20 m3, PTAC 33 t") String specifications,
    @Schema(example = "2026-10-23T08:00:00Z") Instant datePublication,
    @Schema(example = "2026-11-09T17:00:00Z") Instant dateLimite,
    @Schema(example = "PUBLIE") StatutAO statut,
    List<CriterePondereDto> criteres,
    @Schema(example = "false") boolean offreDeposee) {}
