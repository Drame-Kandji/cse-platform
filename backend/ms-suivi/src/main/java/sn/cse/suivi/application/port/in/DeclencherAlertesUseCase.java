package sn.cse.suivi.application.port.in;

/** Contrôles quotidiens planifiés. */
public interface DeclencherAlertesUseCase {

  /**
   * Alerte sur les matériels dont un seuil de maintenance préventive (heures ou jours) est atteint
   * et qui n'ont pas d'ordre de travail ouvert ({@code ALERTE_MAINTENANCE}).
   *
   * @return nombre d'alertes émises
   */
  int alerterMaintenancesPreventives();

  /**
   * Alerte sur les affectations dont la fin prévue est dans la fenêtre J-n ({@code
   * FIN_AFFECTATION_PROCHAINE}), une seule fois par affectation.
   *
   * @return nombre d'alertes émises
   */
  int alerterFinsAffectation();
}
