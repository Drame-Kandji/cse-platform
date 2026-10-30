package sn.cse.besoins.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.besoins.domain.exception.CodesErreurBesoin;
import sn.cse.common.exception.BusinessRuleException;

/**
 * Contenu d'un besoin exprimé par le demandeur (objet-valeur immuable et toujours valide).
 *
 * @param designation désignation du matériel demandé
 * @param categorie catégorie de matériel
 * @param typeEnginId type d'engin du référentiel ms-suivi (optionnel)
 * @param quantite quantité demandée (&gt; 0)
 * @param specifications caractéristiques techniques (optionnel)
 * @param priorite priorité
 * @param dureeUtilisationJours durée d'utilisation prévue en jours (&gt; 0)
 * @param dateBesoin date à laquelle le matériel doit être disponible
 * @param justification justification (obligatoire si URGENT ou CRITIQUE, R08)
 */
public record DescriptionBesoin(
    String designation,
    CategorieMateriel categorie,
    UUID typeEnginId,
    int quantite,
    String specifications,
    Priorite priorite,
    int dureeUtilisationJours,
    LocalDate dateBesoin,
    String justification) {

  public DescriptionBesoin {
    designation = nettoyer(designation);
    specifications = nettoyer(specifications);
    justification = nettoyer(justification);
    if (designation == null) {
      throw invalide("La désignation est obligatoire.");
    }
    Objects.requireNonNull(categorie, "categorie");
    Objects.requireNonNull(priorite, "priorite");
    Objects.requireNonNull(dateBesoin, "dateBesoin");
    if (quantite < 1) {
      throw invalide("La quantité doit être au moins égale à 1.");
    }
    if (dureeUtilisationJours < 1) {
      throw invalide("La durée d'utilisation doit être d'au moins un jour.");
    }
    // Règle R08 : une priorité URGENT ou CRITIQUE doit être justifiée.
    if (priorite.exigeJustification() && justification == null) {
      throw new BusinessRuleException(
          CodesErreurBesoin.JUSTIFICATION_OBLIGATOIRE,
          "Une justification est obligatoire pour un besoin de priorité "
              + priorite
              + " (règle R08).");
    }
  }

  /**
   * @return date de fin d'utilisation prévue ({@code dateBesoin + dureeUtilisationJours})
   */
  public LocalDate dateFinPrevue() {
    return dateBesoin.plusDays(dureeUtilisationJours);
  }

  private static String nettoyer(String valeur) {
    return valeur == null || valeur.isBlank() ? null : valeur.strip();
  }

  private static BusinessRuleException invalide(String message) {
    return new BusinessRuleException(CodesErreurBesoin.BESOIN_INVALIDE, message);
  }
}
