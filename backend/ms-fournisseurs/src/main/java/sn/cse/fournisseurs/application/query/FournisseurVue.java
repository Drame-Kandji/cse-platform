package sn.cse.fournisseurs.application.query;

import java.time.LocalDate;
import sn.cse.fournisseurs.domain.model.Fournisseur;

/**
 * Fournisseur enrichi des indicateurs calculés à la lecture.
 *
 * @param fournisseur agrégat
 * @param nombreLitigesOuverts litiges OUVERT ou EN_COURS
 * @param dateReference date du jour (expiration des documents)
 * @param coordonneesBancairesVisibles l'utilisateur courant peut voir les coordonnées bancaires
 */
public record FournisseurVue(
    Fournisseur fournisseur,
    int nombreLitigesOuverts,
    LocalDate dateReference,
    boolean coordonneesBancairesVisibles) {

  /**
   * @return nombre de documents expirés à la date de référence
   */
  public int documentsExpires() {
    return fournisseur.nombreDocumentsExpires(dateReference);
  }
}
