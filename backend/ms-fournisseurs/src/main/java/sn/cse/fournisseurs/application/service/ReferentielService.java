package sn.cse.fournisseurs.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.DocumentCommand;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.application.port.in.GererReferentielUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.exception.CodesErreur;
import sn.cse.fournisseurs.domain.model.DocumentAdministratif;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/** Cas d'utilisation de gestion du référentiel (création, modification, documents). */
@Service
@Transactional
public class ReferentielService implements GererReferentielUseCase {

  private final FournisseurRepository fournisseurs;
  private final ChargeurFournisseur chargeur;
  private final ScorePolicy politique;
  private final FournisseurEventPublisher evenements;

  /**
   * @param fournisseurs dépôt
   * @param chargeur chargement et vues
   * @param politique politique de score (score initial)
   * @param evenements publication outbox
   */
  public ReferentielService(
      FournisseurRepository fournisseurs,
      ChargeurFournisseur chargeur,
      ScorePolicy politique,
      FournisseurEventPublisher evenements) {
    this.fournisseurs = fournisseurs;
    this.chargeur = chargeur;
    this.politique = politique;
    this.evenements = evenements;
  }

  @Override
  public FournisseurVue creer(FournisseurCommand commande, CurrentUser utilisateur) {
    ProfilFournisseur profil = commande.profil();
    verifierNineaLibre(profil.ninea(), null);
    Fournisseur fournisseur =
        Fournisseur.referencer(
            profil, commande.coordonnees(), politique.scoreInitial(), chargeur.aujourdhui());
    Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
    evenements.publierFournisseur(EventTypes.FOURNISSEUR_INSCRIT, persiste, null);
    return chargeur.vue(persiste, true);
  }

  @Override
  public FournisseurVue modifier(
      UUID id, long version, FournisseurCommand commande, CurrentUser utilisateur) {
    Fournisseur fournisseur = chargeur.charger(id);
    fournisseur.verifierVersion(version);
    ProfilFournisseur profil = commande.profil();
    verifierNineaLibre(profil.ninea(), id);
    fournisseur.modifierProfil(profil, commande.coordonnees());
    Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
    evenements.publierFournisseur(FournisseurEventPublisher.FOURNISSEUR_MODIFIE, persiste, null);
    return chargeur.vue(persiste, true);
  }

  @Override
  public DocumentAdministratif ajouterDocument(UUID id, DocumentCommand commande) {
    Fournisseur fournisseur = chargeur.charger(id);
    DocumentAdministratif document =
        fournisseur.ajouterDocument(commande.type(), commande.libelle(), commande.dateExpiration());
    fournisseurs.sauvegarder(fournisseur);
    return document;
  }

  private void verifierNineaLibre(String ninea, UUID exclu) {
    if (fournisseurs.nineaUtilise(ninea, exclu)) {
      throw new ConflictException(
          CodesErreur.NINEA_DEJA_UTILISE,
          "Le NINEA " + ninea + " est déjà attribué à un fournisseur.");
    }
  }
}
