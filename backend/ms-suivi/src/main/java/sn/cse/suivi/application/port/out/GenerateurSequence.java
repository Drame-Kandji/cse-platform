package sn.cse.suivi.application.port.out;

/** Compteurs annuels sans collision, adossés à des séquences de la base. */
public interface GenerateurSequence {

  /**
   * @param serie nom logique de la série ({@code [a-z_]+}, ex. {@code ot})
   * @param annee année de numérotation
   * @return la prochaine valeur (à partir de 1) de la série pour l'année
   */
  long prochaineValeur(String serie, int annee);
}
