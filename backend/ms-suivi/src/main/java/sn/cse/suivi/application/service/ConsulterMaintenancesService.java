package sn.cse.suivi.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.port.in.ConsulterMaintenancesUseCase;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.query.FiltreMaintenances;
import sn.cse.suivi.application.query.MaintenanceVue;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeMaintenance;

/** Consultation des ordres de travail dans le périmètre de l'utilisateur. */
@Service
@Transactional(readOnly = true)
public class ConsulterMaintenancesService implements ConsulterMaintenancesUseCase {

  private final MaintenanceRepository maintenances;
  private final AssembleurVues vues;

  /**
   * @param maintenances stockage des ordres de travail
   * @param vues assembleur de vues
   */
  public ConsulterMaintenancesService(MaintenanceRepository maintenances, AssembleurVues vues) {
    this.maintenances = maintenances;
    this.vues = vues;
  }

  @Override
  public PageResponse<MaintenanceVue> rechercher(
      StatutMaintenance statut,
      TypeMaintenance type,
      UUID materielId,
      PageQuery page,
      CurrentUser lecteur) {
    FiltreMaintenances filtre =
        new FiltreMaintenances(statut, type, materielId, Perimetre.de(lecteur));
    return AssembleurVues.page(maintenances.rechercher(filtre, page), vues::maintenances);
  }
}
