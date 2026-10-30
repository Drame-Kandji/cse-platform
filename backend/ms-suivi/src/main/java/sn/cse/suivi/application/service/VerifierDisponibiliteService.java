package sn.cse.suivi.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.in.VerifierDisponibiliteUseCase;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.query.CritereDisponibilite;
import sn.cse.suivi.application.query.DisponibiliteVue;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Periode;

/**
 * Disponibilité : matériels {@code DISPONIBLE} (donc sans affectation active, invariant garanti par
 * le stockage), sans demande de réforme, et sans maintenance planifiée pendant la période demandée.
 */
@Service
@Transactional(readOnly = true)
public class VerifierDisponibiliteService implements VerifierDisponibiliteUseCase {

  private final MaterielRepository materiels;
  private final MaintenanceRepository maintenances;
  private final AssembleurVues vues;
  private final SuiviParametres parametres;

  /**
   * @param materiels stockage des matériels
   * @param maintenances stockage des ordres de travail
   * @param vues assembleur de vues
   * @param parametres paramètres métier
   */
  public VerifierDisponibiliteService(
      MaterielRepository materiels,
      MaintenanceRepository maintenances,
      AssembleurVues vues,
      SuiviParametres parametres) {
    this.materiels = materiels;
    this.maintenances = maintenances;
    this.vues = vues;
    this.parametres = parametres;
  }

  @Override
  public DisponibiliteVue verifier(CritereDisponibilite critere) {
    int quantite = critere.quantite() == null ? 1 : Math.max(1, critere.quantite());
    List<Materiel> candidats =
        materiels.rechercherDisponibles(
            critere.categorie(), critere.designation(), parametres.disponibilite().maxResultats());
    long total = materiels.compterDisponibles(critere.categorie(), critere.designation());
    if (critere.du() != null && critere.au() != null) {
      Periode periode = new Periode(critere.du(), critere.au());
      Set<UUID> immobilises =
          maintenances.materielsAvecMaintenancePlanifiee(
              candidats.stream().map(Materiel::getId).toList(), periode.debut(), periode.fin());
      candidats = candidats.stream().filter(m -> !immobilises.contains(m.getId())).toList();
      total -= immobilises.size();
    }
    return new DisponibiliteVue(total >= quantite, quantite, total, vues.materiels(candidats));
  }
}
