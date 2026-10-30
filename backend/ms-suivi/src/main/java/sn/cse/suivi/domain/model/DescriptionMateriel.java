package sn.cse.suivi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Caractéristiques descriptives d'un matériel, modifiables par le Responsable Matériel.
 *
 * @param code code parc unique
 * @param immatriculation immatriculation (engins roulants)
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin du référentiel (optionnel)
 * @param marque marque
 * @param modele modèle
 * @param tarifJournalier coût journalier d'exploitation (FCFA)
 * @param valeurAcquisition valeur d'achat (FCFA)
 * @param dateAcquisition date d'achat
 */
public record DescriptionMateriel(
    String code,
    String immatriculation,
    String designation,
    CategorieMateriel categorie,
    UUID typeEnginId,
    String marque,
    String modele,
    BigDecimal tarifJournalier,
    BigDecimal valeurAcquisition,
    LocalDate dateAcquisition) {

  public DescriptionMateriel {
    if (tarifJournalier == null || tarifJournalier.signum() < 0) {
      throw SuiviErreurs.regle(
          "MONTANT_INVALIDE", "Le tarif journalier est obligatoire et doit être positif.");
    }
    if (valeurAcquisition != null && valeurAcquisition.signum() < 0) {
      throw SuiviErreurs.regle("MONTANT_INVALIDE", "La valeur d'acquisition doit être positive.");
    }
  }
}
