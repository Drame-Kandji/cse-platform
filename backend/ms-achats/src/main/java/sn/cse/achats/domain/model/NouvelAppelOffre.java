package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Données de publication d'un appel d'offres.
 *
 * @param objet objet de la consultation
 * @param description description facultative
 * @param type procédure
 * @param origine besoin d'origine (facultatif)
 * @param categorie catégorie de matériel
 * @param quantite quantité demandée
 * @param specifications spécifications techniques (facultatif)
 * @param budgetEstime budget estimé en FCFA (facultatif)
 * @param dateLimite date limite de dépôt des offres
 * @param ponderation critères pondérés (somme 100)
 * @param invites fournisseurs invités, vérifiés auprès de ms-fournisseurs
 * @param creePar auteur
 */
public record NouvelAppelOffre(
    String objet,
    String description,
    TypeAppelOffre type,
    OrigineBesoin origine,
    CategorieMateriel categorie,
    int quantite,
    String specifications,
    BigDecimal budgetEstime,
    Instant dateLimite,
    PonderationCriteres ponderation,
    List<FournisseurConnu> invites,
    Acteur creePar) {

  public NouvelAppelOffre {
    invites = invites == null ? List.of() : List.copyOf(invites);
  }
}
