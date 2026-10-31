package sn.cse.achats.application.port.out;

/** Génération des références lisibles via une séquence PostgreSQL par année. */
public interface ReferenceGenerator {

  /**
   * @param annee année de publication
   * @return la prochaine référence d'appel d'offres (AO-2026-001)
   */
  String prochaineReferenceAppelOffre(int annee);

  /**
   * @param annee année de génération
   * @return le prochain numéro de contrat (CTR-2026-001)
   */
  String prochainNumeroContrat(int annee);
}
