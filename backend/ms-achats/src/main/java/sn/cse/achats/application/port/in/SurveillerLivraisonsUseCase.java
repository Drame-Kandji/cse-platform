package sn.cse.achats.application.port.in;

/** Détection quotidienne des retards de livraison et calcul des pénalités. */
public interface SurveillerLivraisonsUseCase {

  /**
   * @return nombre d'alertes de retard publiées
   */
  int signalerRetards();
}
