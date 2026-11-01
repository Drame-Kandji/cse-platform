package sn.cse.fournisseurs.application.port.in;

import sn.cse.fournisseurs.application.command.InscriptionCommand;
import sn.cse.fournisseurs.domain.model.Fournisseur;

/** Auto-inscription publique d'un fournisseur (portail, sans authentification, contrat §13). */
public interface InscrireFournisseurUseCase {

  /**
   * Enregistre un fournisseur {@code EN_ATTENTE_VALIDATION} et publie {@code FOURNISSEUR_INSCRIT}.
   * Un NINEA ou un e-mail déjà connu est refusé (409 {@code FOURNISSEUR_EXISTANT}).
   *
   * @param commande données d'inscription
   * @return fournisseur inscrit
   */
  Fournisseur inscrire(InscriptionCommand commande);
}
