package sn.cse.fournisseurs.application.port.out;

/** Génère les références lisibles à partir d'une séquence PostgreSQL annuelle. */
public interface GenerateurReference {

  /**
   * @param annee année de la référence
   * @return référence du prochain litige (ex. {@code LIT-2026-001})
   */
  String prochaineReferenceLitige(int annee);
}
