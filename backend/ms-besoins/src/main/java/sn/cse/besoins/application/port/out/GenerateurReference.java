package sn.cse.besoins.application.port.out;

/** Attribution des références lisibles {@code BES-2026-0001} (séquence par année). */
public interface GenerateurReference {

  /**
   * @param annee année de soumission
   * @return la prochaine référence de l'année
   */
  String prochaineReference(int annee);
}
