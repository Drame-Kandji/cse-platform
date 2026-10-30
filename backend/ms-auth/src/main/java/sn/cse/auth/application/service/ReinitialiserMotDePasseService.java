package sn.cse.auth.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.command.ReinitialiserMotDePasseCommand;
import sn.cse.auth.application.port.in.ReinitialiserMotDePasseUseCase;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.domain.exception.UtilisateurIntrouvableException;

/** Réinitialisation du mot de passe : seul Keycloak le conserve (ADR-0012). */
@Service
@Transactional(readOnly = true)
public class ReinitialiserMotDePasseService implements ReinitialiserMotDePasseUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(ReinitialiserMotDePasseService.class);

  private final UtilisateurRepository repository;
  private final IdentityProviderPort identite;

  /**
   * @param repository profils
   * @param identite fournisseur d'identité
   */
  public ReinitialiserMotDePasseService(
      UtilisateurRepository repository, IdentityProviderPort identite) {
    this.repository = repository;
    this.identite = identite;
  }

  @Override
  public void reinitialiser(ReinitialiserMotDePasseCommand commande) {
    if (repository.parId(commande.id()).isEmpty()) {
      throw new UtilisateurIntrouvableException(commande.id());
    }
    identite.reinitialiserMotDePasse(commande.id(), commande.motDePasseTemporaire());
    LOG.info("Mot de passe temporaire défini pour le compte {}", commande.id());
  }
}
