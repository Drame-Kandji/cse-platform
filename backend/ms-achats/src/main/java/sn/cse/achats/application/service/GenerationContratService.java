package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.GenererContratUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.ReferenceGenerator;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.common.exception.ConflictException;
import sn.cse.events.EventTypes;

/** Génération du contrat BROUILLON d'un marché attribué (R03), au plus un par appel d'offres. */
@Service
@Transactional
public class GenerationContratService implements GenererContratUseCase {

  private final AppelOffreRepository appelsOffres;
  private final ContratRepository contrats;
  private final ReferenceGenerator references;
  private final AchatsEventPublisher evenements;
  private final ParametresAchats parametres;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param contrats dépôt des contrats
   * @param references générateur de numéros
   * @param evenements publication d'événements
   * @param parametres paramètres des contrats
   * @param clock horloge
   */
  public GenerationContratService(
      AppelOffreRepository appelsOffres,
      ContratRepository contrats,
      ReferenceGenerator references,
      AchatsEventPublisher evenements,
      ParametresAchats parametres,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.contrats = contrats;
    this.references = references;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public ContratAchat generer(UUID appelOffreId) {
    return genererPour(Chargements.appelOffre(appelsOffres, appelOffreId));
  }

  /**
   * @param appelOffre appel d'offres attribué
   * @return le contrat généré
   * @throws ConflictException si un contrat existe déjà pour cet appel d'offres
   */
  public ContratAchat genererPour(AppelOffre appelOffre) {
    contrats
        .parAppelOffre(appelOffre.getId())
        .ifPresent(
            c -> {
              throw new ConflictException(
                  CodesErreur.CONTRAT_EXISTANT,
                  "Le contrat " + c.getNumero() + " existe déjà pour cet appel d'offres.");
            });
    int annee = LocalDate.now(clock).getYear();
    // ContratAchat.generer applique R03 avant toute réservation de numéro.
    ContratAchat brouillon =
        ContratAchat.generer(
            appelOffre, () -> references.prochainNumeroContrat(annee), parametres.contrat());
    ContratAchat enregistre = contrats.enregistrer(brouillon);
    evenements.contrat(EventTypes.CONTRAT_GENERE, enregistre);
    return enregistre;
  }
}
