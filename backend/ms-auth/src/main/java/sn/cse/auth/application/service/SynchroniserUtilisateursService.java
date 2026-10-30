package sn.cse.auth.application.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.cse.auth.application.port.in.SynchroniserUtilisateursUseCase;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.query.ResultatSynchronisation;

/**
 * Synchronisation Keycloak → profils. La lecture Keycloak a lieu hors transaction (pas de connexion
 * base retenue pendant les appels HTTP) ; l'import est transactionnel ({@link ImporteurComptes}).
 */
@Service
public class SynchroniserUtilisateursService implements SynchroniserUtilisateursUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(SynchroniserUtilisateursService.class);

  private final IdentityProviderPort identite;
  private final ImporteurComptes importeur;

  /**
   * @param identite fournisseur d'identité
   * @param importeur import transactionnel
   */
  public SynchroniserUtilisateursService(
      IdentityProviderPort identite, ImporteurComptes importeur) {
    this.identite = identite;
    this.importeur = importeur;
  }

  @Override
  public ResultatSynchronisation synchroniser() {
    List<CompteIdentite> comptes = identite.listerComptes();
    ResultatSynchronisation resultat = importeur.importer(comptes);
    LOG.info(
        "Synchronisation Keycloak : {} comptes lus, {} créés, {} modifiés, {} désactivés",
        comptes.size(),
        resultat.crees(),
        resultat.modifies(),
        resultat.desactives());
    return resultat;
  }
}
