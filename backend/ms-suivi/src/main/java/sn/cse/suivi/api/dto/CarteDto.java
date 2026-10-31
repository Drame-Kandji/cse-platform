package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.StatutChantier;

/**
 * Carte du parc au format GeoJSON ({@code FeatureCollection}, RFC 7946).
 *
 * @param type toujours {@code FeatureCollection}
 * @param features points (chantiers et matériels)
 */
@Schema(name = "Carte", description = "FeatureCollection GeoJSON des chantiers et matériels")
public record CarteDto(@Schema(example = "FeatureCollection") String type, List<Feature> features) {

  /** Type GeoJSON de la collection. */
  public static final String FEATURE_COLLECTION = "FeatureCollection";

  /**
   * @param features points
   * @return la collection
   */
  public static CarteDto de(List<Feature> features) {
    return new CarteDto(FEATURE_COLLECTION, features);
  }

  /**
   * Point de la carte.
   *
   * @param type toujours {@code Feature}
   * @param geometry géométrie
   * @param properties propriétés
   */
  @Schema(name = "CarteFeature")
  public record Feature(
      @Schema(example = "Feature") String type, Geometrie geometry, Proprietes properties) {

    /**
     * @param longitude longitude
     * @param latitude latitude
     * @param proprietes propriétés
     * @return le point
     */
    public static Feature point(double longitude, double latitude, Proprietes proprietes) {
      return new Feature(
          "Feature", new Geometrie("Point", List.of(longitude, latitude)), proprietes);
    }
  }

  /**
   * Géométrie ponctuelle.
   *
   * @param type toujours {@code Point}
   * @param coordinates {@code [longitude, latitude]}
   */
  @Schema(name = "CarteGeometrie")
  public record Geometrie(
      @Schema(example = "Point") String type,
      @Schema(description = "[longitude, latitude]", example = "[-17.4677, 14.7167]")
          List<Double> coordinates) {}

  /**
   * Propriétés d'un point.
   *
   * @param kind {@code CHANTIER} ou {@code MATERIEL}
   * @param id identifiant
   * @param code code
   * @param libelle nom ou désignation
   * @param categorie catégorie (matériel)
   * @param etat état (matériel)
   * @param statut statut (chantier)
   * @param chantierId chantier d'affectation courant (matériel)
   * @param chantierNom chantier courant (matériel)
   * @param nombreMateriels matériels présents (chantier)
   */
  @Schema(name = "CarteProprietes")
  public record Proprietes(
      @Schema(
              example = "MATERIEL",
              allowableValues = {"CHANTIER", "MATERIEL"})
          String kind,
      @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID id,
      @Schema(example = "PEL-CAT320-01") String code,
      @Schema(example = "Pelle hydraulique sur chenilles 20 t") String libelle,
      @Schema(example = "MATERIEL_ROULANT", nullable = true) CategorieMateriel categorie,
      @Schema(example = "AFFECTE", nullable = true) EtatMateriel etat,
      @Schema(example = "EN_COURS", nullable = true) StatutChantier statut,
      @Schema(example = "c0000000-0000-0000-0000-000000000001", nullable = true) UUID chantierId,
      @Schema(example = "Extension autoroute Dakar-AIBD", nullable = true) String chantierNom,
      @Schema(example = "8", nullable = true) Long nombreMateriels) {}
}
