package sn.cse.suivi.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.port.in.ConsulterPannesUseCase;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.query.FiltrePannes;
import sn.cse.suivi.application.query.PanneVue;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.model.StatutPanne;

/** Consultation des pannes dans le périmètre de l'utilisateur. */
@Service
@Transactional(readOnly = true)
public class ConsulterPannesService implements ConsulterPannesUseCase {

  private final PanneRepository pannes;
  private final AssembleurVues vues;

  /**
   * @param pannes stockage des pannes
   * @param vues assembleur de vues
   */
  public ConsulterPannesService(PanneRepository pannes, AssembleurVues vues) {
    this.pannes = pannes;
    this.vues = vues;
  }

  @Override
  public PageResponse<PanneVue> rechercher(
      StatutPanne statut, UUID chantierId, UUID materielId, PageQuery page, CurrentUser lecteur) {
    FiltrePannes filtre =
        new FiltrePannes(statut, materielId, Perimetre.de(lecteur).restreindreA(chantierId));
    return AssembleurVues.page(pannes.rechercher(filtre, page), vues::pannes);
  }
}
