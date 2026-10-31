package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.domain.model.AppelOffre;

/** Clôture des appels d'offres (manuelle ou automatique à la date limite). */
public interface CloturerAppelOffreUseCase {

  /**
   * @param appelOffreId appel d'offres
   * @return l'appel d'offres clôturé
   */
  AppelOffre cloturer(UUID appelOffreId);

  /**
   * @return nombre d'appels d'offres clôturés automatiquement
   */
  int cloturerEchus();
}
