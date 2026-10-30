package sn.cse.suivi.application.query;

import java.util.List;
import sn.cse.suivi.domain.model.Mouvement;

/**
 * Fiche complète d'un matériel avec ses historiques récents.
 *
 * @param materiel matériel
 * @param affectations dernières affectations
 * @param maintenances derniers ordres de travail
 * @param pannes dernières pannes
 * @param mouvements derniers mouvements
 */
public record MaterielDetailVue(
    MaterielVue materiel,
    List<AffectationVue> affectations,
    List<MaintenanceVue> maintenances,
    List<PanneVue> pannes,
    List<Mouvement> mouvements) {}
