package sn.cse.achats.application.port.in;

import sn.cse.achats.application.command.DeposerOffreCommand;

/** Dépôt d'offres (R02, R14). */
public interface DeposerOffreUseCase {

  /**
   * Saisie par le Responsable des Achats d'une offre reçue hors portail ; le fournisseur est
   * vérifié auprès de ms-fournisseurs.
   *
   * @param commande offre
   * @return l'offre enregistrée (anonymisée tant que l'appel d'offres est publié)
   */
  OffreVue saisirOffre(DeposerOffreCommand commande);

  /**
   * Dépôt par un fournisseur depuis le portail ; son statut est lu dans la projection locale.
   *
   * @param commande offre
   * @return l'offre enregistrée et son appel d'offres
   */
  OffreFournisseur deposerDepuisPortail(DeposerOffreCommand commande);
}
