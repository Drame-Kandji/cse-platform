package sn.cse.reporting.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.HistoriqueParc;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.Horodatage;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.ParcInstantane;
import sn.cse.reporting.domain.model.vue.ParcInstantane.EtatDuJour;

/**
 * Reconstitue l'instantané du parc d'un jour passé à partir de l'historique des affectations et des
 * maintenances (calcul pur, sans écriture : idempotent par construction). Pour chaque matériel et
 * chaque jour :
 *
 * <ul>
 *   <li>non compté avant le premier fait connu (début de sa première affectation ou maintenance, à
 *       défaut jour du dernier événement d'état) ;
 *   <li>réformé à partir du jour de l'événement de réforme ;
 *   <li>sinon en maintenance si une immobilisation couvre le jour ;
 *   <li>sinon affecté si une affectation (transit compris) couvre le jour ;
 *   <li>sinon disponible.
 * </ul>
 *
 * Le chantier retenu est celui de l'affectation couvrant le jour, à défaut celui de la maintenance,
 * sinon aucun.
 */
public final class ReconstitutionParc {

  private final List<MaterielVue> materiels;
  private final Map<UUID, LocalDate> premiersFaits;
  private final Map<UUID, List<MaintenanceVue>> maintenances;
  private final Map<UUID, List<AffectationVue>> affectations;
  private final LocalDate aujourdhui;

  /**
   * @param historique historique complet du parc
   * @param aujourdhui date du jour (fin des intervalles encore ouverts)
   */
  public ReconstitutionParc(HistoriqueParc historique, LocalDate aujourdhui) {
    this.aujourdhui = aujourdhui;
    this.materiels = historique.materiels().stream().filter(m -> m.categorie() != null).toList();
    this.maintenances =
        historique.maintenances().stream()
            .filter(m -> m.materielId() != null && m.immobilisation(aujourdhui).isPresent())
            .collect(Collectors.groupingBy(MaintenanceVue::materielId));
    this.affectations =
        historique.affectations().stream()
            .filter(a -> a.materielId() != null && a.joursOccupes(aujourdhui).isPresent())
            .collect(Collectors.groupingBy(AffectationVue::materielId));
    this.premiersFaits = new HashMap<>();
    for (MaterielVue m : materiels) {
      LocalDate premier = premierFait(m);
      if (premier != null) {
        premiersFaits.put(m.id(), premier);
      }
    }
  }

  /**
   * @param jour jour à reconstituer
   * @return l'instantané reconstitué (une ligne par couple catégorie × chantier), vide si aucun
   *     matériel n'est connu ce jour-là
   */
  public List<ParcInstantane> au(LocalDate jour) {
    List<EtatDuJour> etats = new ArrayList<>();
    for (MaterielVue m : materiels) {
      etat(m, jour).ifPresent(etats::add);
    }
    return ParcInstantane.agreger(jour, etats);
  }

  private Optional<EtatDuJour> etat(MaterielVue m, LocalDate jour) {
    LocalDate premierFait = premiersFaits.get(m.id());
    if (premierFait != null && jour.isBefore(premierFait)) {
      return Optional.empty();
    }
    if (m.reforme() && (m.etatMajLe() == null || !jour.isBefore(Horodatage.jour(m.etatMajLe())))) {
      return Optional.of(new EtatDuJour(m.categorie(), m.chantierId(), EtatMateriel.REFORME));
    }
    Optional<AffectationVue> affectation =
        affectations.getOrDefault(m.id(), List.of()).stream()
            .filter(a -> a.joursOccupes(aujourdhui).orElseThrow().contient(jour))
            .findFirst();
    Optional<MaintenanceVue> maintenance =
        maintenances.getOrDefault(m.id(), List.of()).stream()
            .filter(x -> x.immobilisation(aujourdhui).orElseThrow().contient(jour))
            .findFirst();
    UUID chantier =
        affectation
            .map(AffectationVue::chantierId)
            .orElseGet(() -> maintenance.map(MaintenanceVue::chantierId).orElse(null));
    EtatMateriel etat;
    if (maintenance.isPresent()) {
      etat = EtatMateriel.EN_MAINTENANCE;
    } else if (affectation.isPresent()) {
      etat = EtatMateriel.AFFECTE;
    } else {
      etat = EtatMateriel.DISPONIBLE;
    }
    return Optional.of(new EtatDuJour(m.categorie(), chantier, etat));
  }

  private LocalDate premierFait(MaterielVue m) {
    LocalDate premier = m.etatMajLe() == null ? null : Horodatage.jour(m.etatMajLe());
    for (AffectationVue a : affectations.getOrDefault(m.id(), List.of())) {
      premier = Horodatage.min(premier, a.joursOccupes(aujourdhui).orElseThrow().du());
    }
    for (MaintenanceVue x : maintenances.getOrDefault(m.id(), List.of())) {
      premier = Horodatage.min(premier, x.immobilisation(aujourdhui).orElseThrow().du());
    }
    return premier;
  }
}
