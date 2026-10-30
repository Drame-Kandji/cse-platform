package sn.cse.fournisseurs.domain.model;

import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;

/**
 * Coordonnées bancaires d'un fournisseur — donnée sensible, chiffrée au repos (AES-256-GCM) et
 * visible uniquement du Responsable des Achats et de la DAF.
 *
 * @param banque nom de la banque
 * @param iban IBAN (sans espaces, en majuscules)
 */
public record CoordonneesBancaires(String banque, String iban) {

  /** Normalise l'IBAN et vérifie la présence des deux champs. */
  public CoordonneesBancaires {
    if (banque == null || banque.isBlank() || iban == null || iban.isBlank()) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "La banque et l'IBAN sont obligatoires.");
    }
    banque = banque.trim();
    iban = iban.replace(" ", "").toUpperCase(java.util.Locale.ROOT);
  }
}
