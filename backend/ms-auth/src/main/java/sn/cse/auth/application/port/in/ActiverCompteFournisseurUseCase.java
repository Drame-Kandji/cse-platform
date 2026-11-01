package sn.cse.auth.application.port.in;

import sn.cse.auth.application.command.ActiverCompteFournisseurCommand;

/**
 * Création (ou mise en conformité) du compte du portail d'un fournisseur dont l'inscription vient
 * d'être validée par le Responsable des Achats (contrat §13).
 */
public interface ActiverCompteFournisseurUseCase {

  /**
   * Idempotent : un compte existant reçoit seulement le rôle {@code FOURNISSEUR} et l'attribut
   * {@code fournisseur_id} manquants.
   *
   * @param commande fournisseur validé
   */
  void activer(ActiverCompteFournisseurCommand commande);
}
