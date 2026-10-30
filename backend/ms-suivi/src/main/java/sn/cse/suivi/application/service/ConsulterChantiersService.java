package sn.cse.suivi.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.port.in.ConsulterChantiersUseCase;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.application.query.FiltreChantiers;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.StatutChantier;

/** Consultation des chantiers dans le périmètre de l'utilisateur. */
@Service
@Transactional(readOnly = true)
public class ConsulterChantiersService implements ConsulterChantiersUseCase {

  private final ChantierRepository chantiers;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;

  /**
   * @param chantiers stockage des chantiers
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   */
  public ConsulterChantiersService(
      ChantierRepository chantiers, ChargeurAgregats chargeur, AssembleurVues vues) {
    this.chantiers = chantiers;
    this.chargeur = chargeur;
    this.vues = vues;
  }

  @Override
  public PageResponse<ChantierVue> rechercher(
      StatutChantier statut, String q, PageQuery page, CurrentUser lecteur) {
    FiltreChantiers filtre = new FiltreChantiers(statut, q, Perimetre.de(lecteur));
    return AssembleurVues.page(chantiers.rechercher(filtre, page), vues::chantiers);
  }

  @Override
  public ChantierVue consulter(UUID id, CurrentUser lecteur) {
    if (!lecteur.peutAccederAuChantier(id)) {
      throw SuiviErreurs.chantierNonAutorise(id);
    }
    return vues.chantier(chargeur.chantier(id));
  }
}
