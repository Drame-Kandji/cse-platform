package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.CloturerAppelOffreUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.events.EventTypes;

/** Clôture des appels d'offres : à la demande du RA ou automatiquement à la date limite. */
@Service
@Transactional
public class ClotureAppelOffreService implements CloturerAppelOffreUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(ClotureAppelOffreService.class);

  private final AppelOffreRepository appelsOffres;
  private final AchatsEventPublisher evenements;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param evenements publication d'événements
   * @param clock horloge
   */
  public ClotureAppelOffreService(
      AppelOffreRepository appelsOffres, AchatsEventPublisher evenements, Clock clock) {
    this.appelsOffres = appelsOffres;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public AppelOffre cloturer(UUID appelOffreId) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, appelOffreId);
    return cloturer(appelOffre, Instant.now(clock));
  }

  @Override
  public int cloturerEchus() {
    Instant maintenant = Instant.now(clock);
    int n = 0;
    for (UUID id : appelsOffres.idsPubliesEchus(maintenant)) {
      AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, id);
      if (appelOffre.estEchu(maintenant)) {
        cloturer(appelOffre, maintenant);
        n++;
      }
    }
    if (n > 0) {
      LOG.info("Clôture automatique de {} appel(s) d'offres à date limite", n);
    }
    return n;
  }

  private AppelOffre cloturer(AppelOffre appelOffre, Instant maintenant) {
    appelOffre.cloturer(maintenant);
    AppelOffre enregistre = appelsOffres.enregistrer(appelOffre);
    evenements.appelOffre(EventTypes.AO_CLOTURE, enregistre);
    return enregistre;
  }
}
