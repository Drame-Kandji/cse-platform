package sn.cse.fournisseurs.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.query.FournisseurVue;

/** Transitions du cycle de vie d'un fournisseur (machine à états, R09). */
public interface GererCycleVieUseCase {

  /**
   * {@code EN_ATTENTE_VALIDATION → ACTIF}.
   *
   * @param id fournisseur
   * @param utilisateur utilisateur courant
   * @return fournisseur
   */
  FournisseurVue validerInscription(UUID id, CurrentUser utilisateur);

  /**
   * {@code ACTIF → SUSPENDU}.
   *
   * @param id fournisseur
   * @param motif motif obligatoire
   * @param utilisateur utilisateur courant
   * @return fournisseur
   */
  FournisseurVue suspendre(UUID id, String motif, CurrentUser utilisateur);

  /**
   * {@code SUSPENDU → ACTIF}.
   *
   * @param id fournisseur
   * @param utilisateur utilisateur courant
   * @return fournisseur
   */
  FournisseurVue reactiver(UUID id, CurrentUser utilisateur);

  /**
   * R09 étape 1 : demande de blacklistage par le Responsable des Achats.
   *
   * @param id fournisseur
   * @param motif motif obligatoire
   * @param utilisateur demandeur
   * @return fournisseur
   */
  FournisseurVue demanderBlacklistage(UUID id, String motif, CurrentUser utilisateur);

  /**
   * R09 étape 2 : décision de la Direction Générale.
   *
   * @param id fournisseur
   * @param approuve {@code true} pour blacklister
   * @param commentaire commentaire (optionnel)
   * @param utilisateur décideur
   * @return fournisseur
   */
  FournisseurVue deciderBlacklistage(
      UUID id, boolean approuve, String commentaire, CurrentUser utilisateur);
}
