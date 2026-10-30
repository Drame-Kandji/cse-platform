package sn.cse.fournisseurs.application.port.in;

import java.util.UUID;
import sn.cse.fournisseurs.application.command.LivraisonCommand;

/** Mise à jour automatique des indicateurs de performance à partir des événements (R07). */
public interface SuiviPerformanceUseCase {

  /**
   * Prend en compte une livraison ({@code LIVRAISON_EFFECTUEE}) puis recalcule le score.
   *
   * @param commande livraison constatée
   */
  void enregistrerLivraison(LivraisonCommand commande);

  /**
   * Recalcule le score après une réception confirmée sur chantier ({@code RECEPTION_CONFIRMEE}).
   *
   * @param fournisseurId fournisseur
   */
  void receptionConfirmee(UUID fournisseurId);
}
