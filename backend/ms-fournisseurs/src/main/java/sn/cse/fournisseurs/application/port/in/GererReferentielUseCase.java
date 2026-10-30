package sn.cse.fournisseurs.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.command.DocumentCommand;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.DocumentAdministratif;

/** Gestion du référentiel fournisseurs (Responsable des Achats). */
public interface GererReferentielUseCase {

  /**
   * Référence un fournisseur (statut ACTIF).
   *
   * @param commande données
   * @param utilisateur utilisateur courant
   * @return fournisseur créé
   */
  FournisseurVue creer(FournisseurCommand commande, CurrentUser utilisateur);

  /**
   * Met à jour le référentiel (verrou optimiste sur {@code version}).
   *
   * @param id fournisseur
   * @param version version connue du client
   * @param commande données
   * @param utilisateur utilisateur courant
   * @return fournisseur modifié
   */
  FournisseurVue modifier(
      UUID id, long version, FournisseurCommand commande, CurrentUser utilisateur);

  /**
   * Ajoute un document administratif.
   *
   * @param id fournisseur
   * @param commande document
   * @return document créé
   */
  DocumentAdministratif ajouterDocument(UUID id, DocumentCommand commande);
}
