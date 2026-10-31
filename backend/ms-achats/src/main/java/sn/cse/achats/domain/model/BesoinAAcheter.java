package sn.cse.achats.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ConflictException;

/**
 * Projection locale d'un besoin orienté vers l'achat (événement {@code BESOIN_EN_ACHAT} de
 * ms-besoins).
 *
 * @param besoinId identifiant du besoin
 * @param reference référence lisible
 * @param designation désignation
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param priorite priorité
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param dateReception instant de réception de l'événement
 * @param appelOffreId appel d'offres lancé pour ce besoin
 */
public record BesoinAAcheter(
    UUID besoinId,
    String reference,
    String designation,
    CategorieMateriel categorie,
    int quantite,
    String specifications,
    Priorite priorite,
    UUID chantierId,
    String chantierNom,
    Instant dateReception,
    UUID appelOffreId) {

  public BesoinAAcheter {
    Objects.requireNonNull(besoinId, "besoinId");
    Objects.requireNonNull(dateReception, "dateReception");
  }

  /**
   * @param aoId appel d'offres lancé
   * @return le besoin rattaché
   * @throws ConflictException si un appel d'offres est déjà rattaché
   */
  public BesoinAAcheter rattacherA(UUID aoId) {
    if (appelOffreId != null) {
      throw new ConflictException(
          CodesErreur.BESOIN_DEJA_EN_ACHAT,
          "Le besoin " + reference + " fait déjà l'objet d'un appel d'offres.");
    }
    return new BesoinAAcheter(
        besoinId,
        reference,
        designation,
        categorie,
        quantite,
        specifications,
        priorite,
        chantierId,
        chantierNom,
        dateReception,
        aoId);
  }

  /**
   * @return l'origine à reporter sur l'appel d'offres
   */
  public OrigineBesoin commeOrigine() {
    return new OrigineBesoin(besoinId, reference, chantierId, chantierNom, designation);
  }
}
