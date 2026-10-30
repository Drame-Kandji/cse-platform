package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.command.CloturerMaintenanceCommande;
import sn.cse.suivi.application.command.PlanifierMaintenanceCommande;
import sn.cse.suivi.application.query.MaintenanceVue;

/** Cycle de vie des ordres de travail : {@code PLANIFIEE → EN_COURS → CLOTUREE}. */
public interface GererMaintenancesUseCase {

  /**
   * @param commande demande
   * @return l'ordre de travail planifié (numéro {@code OT-AAAA-NNNN})
   */
  MaintenanceVue planifier(PlanifierMaintenanceCommande commande);

  /**
   * @param maintenanceId ordre de travail
   * @param auteur utilisateur
   * @return l'ordre de travail en cours (matériel {@code EN_MAINTENANCE})
   */
  MaintenanceVue demarrer(UUID maintenanceId, CurrentUser auteur);

  /**
   * @param commande demande
   * @return l'ordre de travail clôturé (matériel remis en service)
   */
  MaintenanceVue cloturer(CloturerMaintenanceCommande commande);
}
