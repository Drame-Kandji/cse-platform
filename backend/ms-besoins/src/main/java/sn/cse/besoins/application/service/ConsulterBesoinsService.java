package sn.cse.besoins.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.port.in.ConsulterBesoinsUseCase;
import sn.cse.besoins.application.port.out.BesoinQueryPort;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.application.query.CriteresRechercheBesoins;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.HistoriqueBesoin;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/** Consultation des besoins avec filtrage par chantier pour les chefs de chantier. */
@Service
@Transactional(readOnly = true)
public class ConsulterBesoinsService implements ConsulterBesoinsUseCase {

  private final EnregistrementBesoin enregistrement;
  private final BesoinQueryPort requetes;

  ConsulterBesoinsService(EnregistrementBesoin enregistrement, BesoinQueryPort requetes) {
    this.enregistrement = enregistrement;
    this.requetes = requetes;
  }

  @Override
  public BesoinVue obtenir(UUID id, CurrentUser utilisateur) {
    return BesoinVue.depuis(chargerVisible(id, utilisateur));
  }

  @Override
  public PageResponse<BesoinVue> rechercher(
      CriteresRechercheBesoins criteres, PageQuery page, CurrentUser utilisateur) {
    CriteresRechercheBesoins effectifs = criteres;
    if (utilisateur.estRestreintAuxChantiers()) {
      if (criteres.chantierId() != null) {
        ControleAccesBesoin.verifierChantierAttribue(criteres.chantierId(), utilisateur);
      }
      effectifs = criteres.restreintA(utilisateur.chantierIds());
    }
    return requetes.rechercher(effectifs, page);
  }

  @Override
  public List<HistoriqueBesoin> historique(UUID id, CurrentUser utilisateur) {
    return chargerVisible(id, utilisateur).historique();
  }

  private BesoinMateriel chargerVisible(UUID id, CurrentUser utilisateur) {
    BesoinMateriel besoin = enregistrement.charger(id);
    ControleAccesBesoin.verifierLecture(besoin, utilisateur);
    return besoin;
  }
}
