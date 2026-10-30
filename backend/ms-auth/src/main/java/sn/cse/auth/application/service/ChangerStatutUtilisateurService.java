package sn.cse.auth.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.command.ChangerStatutCommand;
import sn.cse.auth.application.port.in.ChangerStatutUtilisateurUseCase;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.domain.exception.OperationSurSoiInterditeException;
import sn.cse.auth.domain.exception.UtilisateurIntrouvableException;
import sn.cse.auth.domain.model.Utilisateur;

/** Activation / désactivation d'un compte (bloque la connexion Keycloak). */
@Service
@Transactional
public class ChangerStatutUtilisateurService implements ChangerStatutUtilisateurUseCase {

  private final UtilisateurRepository repository;
  private final IdentityProviderPort identite;
  private final UtilisateurEventPublisher events;

  /**
   * @param repository profils
   * @param identite fournisseur d'identité
   * @param events publication des événements
   */
  public ChangerStatutUtilisateurService(
      UtilisateurRepository repository,
      IdentityProviderPort identite,
      UtilisateurEventPublisher events) {
    this.repository = repository;
    this.identite = identite;
    this.events = events;
  }

  @Override
  public Utilisateur changerStatut(ChangerStatutCommand commande) {
    if (!commande.actif() && commande.id().equals(commande.auteurId())) {
      throw new OperationSurSoiInterditeException();
    }
    Utilisateur utilisateur =
        repository
            .parId(commande.id())
            .orElseThrow(() -> new UtilisateurIntrouvableException(commande.id()));
    if (utilisateur.actif() == commande.actif()) {
      return utilisateur;
    }
    utilisateur.definirActif(commande.actif());
    Utilisateur enregistre = repository.enregistrer(utilisateur);
    identite.definirActif(commande.id(), commande.actif());
    events.publierModifie(enregistre);
    return enregistre;
  }
}
