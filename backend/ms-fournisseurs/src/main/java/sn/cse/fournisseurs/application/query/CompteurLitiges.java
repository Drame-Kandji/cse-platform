package sn.cse.fournisseurs.application.query;

/**
 * Nombre de litiges d'un fournisseur par état.
 *
 * @param ouverts litiges OUVERT ou EN_COURS
 * @param resolus litiges RESOLU
 * @param classes litiges CLASSE
 */
public record CompteurLitiges(int ouverts, int resolus, int classes) {

  /** Aucun litige. */
  public static final CompteurLitiges AUCUN = new CompteurLitiges(0, 0, 0);

  /**
   * @return nombre total de litiges
   */
  public int total() {
    return ouverts + resolus + classes;
  }
}
