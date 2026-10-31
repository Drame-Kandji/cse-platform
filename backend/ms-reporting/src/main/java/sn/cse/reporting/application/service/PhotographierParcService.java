package sn.cse.reporting.application.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.reporting.application.port.in.PhotographierParcUseCase;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/**
 * Photographie quotidienne du parc : appelée par le job planifié et après chaque événement
 * modifiant l'état d'un matériel, afin que la série de disponibilité existe dans le temps.
 */
@Service
public class PhotographierParcService implements PhotographierParcUseCase {

  private final ProjectionRepository vues;

  /**
   * @param vues persistance des vues
   */
  public PhotographierParcService(ProjectionRepository vues) {
    this.vues = vues;
  }

  @Override
  @Transactional
  public void photographier(LocalDate jour) {
    vues.remplacerInstantanes(jour, ParcInstantane.photographier(jour, vues.materiels()));
  }
}
