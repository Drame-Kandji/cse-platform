package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.application.command.ProposerAttributionCommand;
import sn.cse.achats.application.port.in.AttribuerMarcheUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ResultatDecision;
import sn.cse.events.EventTypes;

/**
 * Circuit d'attribution (R11) : proposition du RA, co-validation du RM, approbation DG au-delà du
 * seuil de délégation. Lorsque toutes les approbations sont acquises, le marché est attribué et le
 * contrat BROUILLON est généré dans la même transaction (R03).
 */
@Service
@Transactional
public class AttributionMarcheService implements AttribuerMarcheUseCase {

  private final AppelOffreRepository appelsOffres;
  private final GenerationContratService contrats;
  private final AchatsEventPublisher evenements;
  private final ParametresAchats parametres;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param contrats génération des contrats
   * @param evenements publication d'événements
   * @param parametres seuil de délégation
   * @param clock horloge
   */
  public AttributionMarcheService(
      AppelOffreRepository appelsOffres,
      GenerationContratService contrats,
      AchatsEventPublisher evenements,
      ParametresAchats parametres,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.contrats = contrats;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public AppelOffre proposer(ProposerAttributionCommand commande) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, commande.appelOffreId());
    appelOffre.proposerAttribution(
        commande.offreId(),
        commande.justification(),
        Acteurs.de(commande.auteur()),
        Instant.now(clock),
        parametres.seuilDelegation());
    AppelOffre enregistre = appelsOffres.enregistrer(appelOffre);
    evenements.appelOffre(EventTypes.ATTRIBUTION_PROPOSEE, enregistre);
    return enregistre;
  }

  @Override
  public AppelOffre coValider(DecisionCommand commande) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, commande.cibleId());
    ResultatDecision resultat =
        appelOffre.coValiderAttribution(
            commande.approuve(),
            commande.commentaire(),
            Acteurs.de(commande.decideur()),
            Instant.now(clock));
    return conclure(appelOffre, resultat);
  }

  @Override
  public AppelOffre approuver(DecisionCommand commande) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, commande.cibleId());
    ResultatDecision resultat =
        appelOffre.approuverAttribution(
            commande.approuve(),
            commande.commentaire(),
            Acteurs.de(commande.decideur()),
            Instant.now(clock));
    return conclure(appelOffre, resultat);
  }

  private AppelOffre conclure(AppelOffre appelOffre, ResultatDecision resultat) {
    AppelOffre enregistre = appelsOffres.enregistrer(appelOffre);
    switch (resultat) {
      case ATTRIBUTION_REFUSEE -> evenements.appelOffre(EventTypes.ATTRIBUTION_REFUSEE, enregistre);
      case MARCHE_ATTRIBUE -> {
        evenements.appelOffre(EventTypes.MARCHE_ATTRIBUE, enregistre);
        contrats.genererPour(enregistre);
      }
      case EN_ATTENTE_AUTRES_APPROBATIONS -> {
        // Aucun événement : la proposition reste en attente des autres approbateurs.
      }
    }
    return enregistre;
  }
}
