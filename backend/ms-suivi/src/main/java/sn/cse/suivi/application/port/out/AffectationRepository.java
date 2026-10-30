package sn.cse.suivi.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.FiltreAffectations;
import sn.cse.suivi.application.query.FiltrePlanning;
import sn.cse.suivi.domain.model.Affectation;

/** Stockage des affectations. */
public interface AffectationRepository {

  /**
   * Enregistre une affectation. Le stockage garantit qu'un matériel n'a jamais deux affectations
   * actives, y compris sous accès concurrents.
   *
   * @param affectation affectation
   * @return l'état enregistré
   * @throws sn.cse.common.exception.ConflictException {@code CONFLIT_AFFECTATION} si une autre
   *     affectation active du matériel a été enregistrée simultanément
   */
  Affectation save(Affectation affectation);

  /**
   * @param id identifiant
   * @return l'affectation si elle existe
   */
  Optional<Affectation> findById(UUID id);

  /**
   * @param materielId matériel
   * @return affectations actives (non terminées) du matériel
   */
  List<Affectation> findActivesByMateriel(UUID materielId);

  /**
   * @param materielId matériel
   * @return l'affectation active du matériel, s'il y en a une
   */
  default Optional<Affectation> findActiveByMateriel(UUID materielId) {
    return findActivesByMateriel(materielId).stream().findFirst();
  }

  /**
   * @param materielId matériel
   * @param limite nombre maximal
   * @return dernières affectations du matériel (plus récentes d'abord)
   */
  List<Affectation> findRecentesByMateriel(UUID materielId, int limite);

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page d'affectations
   */
  PageResponse<Affectation> rechercher(FiltreAffectations filtre, PageQuery page);

  /**
   * @param filtre critères (période obligatoire)
   * @return affectations dont la période effective chevauche la période demandée
   */
  List<Affectation> planning(FiltrePlanning filtre);

  /**
   * @param limite date de fin prévue maximale
   * @return affectations actives dont la fin prévue est au plus tard {@code limite}
   */
  List<Affectation> findActivesFinissantAuPlusTard(LocalDate limite);

  /**
   * @param chantierId chantier
   * @return nombre d'affectations actives sur le chantier
   */
  long compterActivesParChantier(UUID chantierId);
}
