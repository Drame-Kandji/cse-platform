package sn.cse.besoins.application.command;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Contenu saisi d'un besoin (création ou modification).
 *
 * @param chantierId chantier
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin (optionnel)
 * @param quantite quantité
 * @param specifications spécifications (optionnel)
 * @param priorite priorité
 * @param dureeUtilisationJours durée d'utilisation
 * @param dateBesoin date de besoin
 * @param justification justification (R08)
 */
public record ContenuBesoin(
    UUID chantierId,
    String designation,
    CategorieMateriel categorie,
    UUID typeEnginId,
    int quantite,
    String specifications,
    Priorite priorite,
    int dureeUtilisationJours,
    LocalDate dateBesoin,
    String justification) {

  /**
   * @return l'objet-valeur du domaine (applique R08)
   */
  public DescriptionBesoin description() {
    return new DescriptionBesoin(
        designation,
        categorie,
        typeEnginId,
        quantite,
        specifications,
        priorite,
        dureeUtilisationJours,
        dateBesoin,
        justification);
  }
}
