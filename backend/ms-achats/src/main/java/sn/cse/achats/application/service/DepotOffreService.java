package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.DeposerOffreCommand;
import sn.cse.achats.application.port.in.DeposerOffreUseCase;
import sn.cse.achats.application.port.in.OffreFournisseur;
import sn.cse.achats.application.port.in.OffreVue;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.FournisseurAnnuaire;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.service.OffreEvaluee;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.events.EventTypes;

/**
 * Dépôt des offres : règles R02 (date limite) et R14 (fournisseur éligible) portées par l'agrégat.
 */
@Service
@Transactional
public class DepotOffreService implements DeposerOffreUseCase {

  private final AppelOffreRepository appelsOffres;
  private final FournisseurAnnuaire annuaire;
  private final FournisseurProjectionRepository projection;
  private final AchatsEventPublisher evenements;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param annuaire vérification auprès de ms-fournisseurs
   * @param projection projection locale des fournisseurs
   * @param evenements publication d'événements
   * @param clock horloge
   */
  public DepotOffreService(
      AppelOffreRepository appelsOffres,
      FournisseurAnnuaire annuaire,
      FournisseurProjectionRepository projection,
      AchatsEventPublisher evenements,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.annuaire = annuaire;
    this.projection = projection;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public OffreVue saisirOffre(DeposerOffreCommand commande) {
    FournisseurConnu fournisseur =
        annuaire
            .verifier(commande.fournisseurId())
            .orElseThrow(() -> inconnu(commande.fournisseurId()));
    AppelOffre appelOffre = deposer(commande, fournisseur);
    Offre offre = appelOffre.offreDu(fournisseur.id()).orElseThrow();
    return new OffreVue(appelOffre.getId(), appelOffre.estAnonyme(), OffreEvaluee.sansNote(offre));
  }

  @Override
  public OffreFournisseur deposerDepuisPortail(DeposerOffreCommand commande) {
    // Le jeton d'un fournisseur n'ouvre pas ms-fournisseurs : son statut est lu localement.
    FournisseurConnu fournisseur =
        projection
            .parId(commande.fournisseurId())
            .orElseThrow(() -> inconnu(commande.fournisseurId()));
    AppelOffre appelOffre = deposer(commande, fournisseur);
    return new OffreFournisseur(appelOffre, appelOffre.offreDu(fournisseur.id()).orElseThrow());
  }

  private AppelOffre deposer(DeposerOffreCommand commande, FournisseurConnu fournisseur) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, commande.appelOffreId());
    appelOffre.deposerOffre(
        fournisseur,
        commande.montantPropose(),
        commande.delaiLivraisonJours(),
        commande.garantieMois(),
        commande.description(),
        Instant.now(clock));
    AppelOffre enregistre = appelsOffres.enregistrer(appelOffre);
    evenements.appelOffre(EventTypes.OFFRE_DEPOSEE, enregistre);
    return enregistre;
  }

  private static BusinessRuleException inconnu(UUID id) {
    return new BusinessRuleException(
        CodesErreur.FOURNISSEUR_INCONNU, "Fournisseur inconnu : " + id + ".");
  }
}
