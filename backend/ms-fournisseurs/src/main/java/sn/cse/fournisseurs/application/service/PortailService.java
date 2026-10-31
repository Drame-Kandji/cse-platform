package sn.cse.fournisseurs.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.application.port.in.GererCatalogueUseCase;
import sn.cse.fournisseurs.application.port.in.GererReferentielUseCase;
import sn.cse.fournisseurs.application.port.in.PortailFournisseurUseCase;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;

/**
 * Portail fournisseur : contrôle d'accès « données », un compte FOURNISSEUR n'agit que sur le
 * fournisseur de son claim {@code fournisseur_id}.
 */
@Service
@Transactional
public class PortailService implements PortailFournisseurUseCase {

  /** Code d'erreur d'un compte sans fournisseur associé (403). */
  static final String FOURNISSEUR_NON_ASSOCIE = "FOURNISSEUR_NON_ASSOCIE";

  private final ChargeurFournisseur chargeur;
  private final GererReferentielUseCase referentiel;
  private final GererCatalogueUseCase catalogue;

  /**
   * @param chargeur chargement des fournisseurs
   * @param referentiel gestion du référentiel
   * @param catalogue gestion du catalogue
   */
  public PortailService(
      ChargeurFournisseur chargeur,
      GererReferentielUseCase referentiel,
      GererCatalogueUseCase catalogue) {
    this.chargeur = chargeur;
    this.referentiel = referentiel;
    this.catalogue = catalogue;
  }

  @Override
  @Transactional(readOnly = true)
  public FournisseurVue profil(CurrentUser utilisateur) {
    return chargeur.vue(chargeur.charger(fournisseurDe(utilisateur)), true);
  }

  @Override
  public FournisseurVue modifierProfil(
      CurrentUser utilisateur, long version, FournisseurCommand commande) {
    return referentiel.modifier(fournisseurDe(utilisateur), version, commande, utilisateur);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LigneCatalogue> catalogue(CurrentUser utilisateur) {
    return catalogue.catalogue(fournisseurDe(utilisateur));
  }

  @Override
  public LigneCatalogue publierLigne(CurrentUser utilisateur, LigneCatalogueCommand commande) {
    return catalogue.publierLigne(fournisseurDe(utilisateur), commande);
  }

  private static UUID fournisseurDe(CurrentUser utilisateur) {
    return utilisateur
        .fournisseur()
        .orElseThrow(
            () ->
                new ForbiddenOperationException(
                    FOURNISSEUR_NON_ASSOCIE,
                    "Ce compte n'est associé à aucun fournisseur (claim fournisseur_id absent)."));
  }
}
