package sn.cse.reporting.domain.model;

import java.util.List;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;

/**
 * Historique complet du parc (tous chantiers, toutes dates) : base de la reconstitution des
 * instantanés quotidiens antérieurs au premier instantané enregistré.
 *
 * @param materiels matériels connus
 * @param affectations toutes les affectations
 * @param maintenances toutes les maintenances
 */
public record HistoriqueParc(
    List<MaterielVue> materiels,
    List<AffectationVue> affectations,
    List<MaintenanceVue> maintenances) {

  public HistoriqueParc {
    materiels = List.copyOf(materiels);
    affectations = List.copyOf(affectations);
    maintenances = List.copyOf(maintenances);
  }

  /**
   * @return un historique vide (aucune reconstitution)
   */
  public static HistoriqueParc vide() {
    return new HistoriqueParc(List.of(), List.of(), List.of());
  }
}
