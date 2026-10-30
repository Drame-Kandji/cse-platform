package sn.cse.fournisseurs.application.service;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.fournisseurs.application.command.LivraisonCommand;
import sn.cse.fournisseurs.application.port.in.SuiviPerformanceUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.model.Fournisseur;

/**
 * Mise à jour des indicateurs à partir des événements des autres services (R07) : livraisons
 * (ponctualité) et réceptions confirmées sur chantier.
 */
@Service
@Transactional
public class PerformanceService implements SuiviPerformanceUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(PerformanceService.class);

  private final FournisseurRepository fournisseurs;
  private final RecalculScore recalcul;

  /**
   * @param fournisseurs dépôt
   * @param recalcul recalcul du score
   */
  public PerformanceService(FournisseurRepository fournisseurs, RecalculScore recalcul) {
    this.fournisseurs = fournisseurs;
    this.recalcul = recalcul;
  }

  @Override
  public void enregistrerLivraison(LivraisonCommand commande) {
    trouver(commande.fournisseurId())
        .ifPresent(
            f -> {
              f.enregistrerLivraison(
                  commande.dateLivraisonPrevue(), commande.dateLivraisonReelle());
              recalcul.recalculer(f, "Livraison du contrat " + commande.numeroContrat());
            });
  }

  @Override
  public void receptionConfirmee(UUID fournisseurId) {
    trouver(fournisseurId)
        .ifPresent(f -> recalcul.recalculer(f, "Réception confirmée sur chantier"));
  }

  private Optional<Fournisseur> trouver(UUID id) {
    if (id == null) {
      return Optional.empty();
    }
    Optional<Fournisseur> fournisseur = fournisseurs.parId(id);
    if (fournisseur.isEmpty()) {
      // Un événement peut concerner un fournisseur inconnu localement (données d'un autre
      // environnement) : il est ignoré sans rejeu.
      LOG.warn("Événement ignoré : fournisseur {} inconnu", id);
    }
    return fournisseur;
  }
}
