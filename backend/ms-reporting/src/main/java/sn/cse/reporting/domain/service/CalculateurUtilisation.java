package sn.cse.reporting.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;

/**
 * Taux d'utilisation = jours affectés / jours disponibles, calculé jour par jour et matériel par
 * matériel sur la partie écoulée de la période :
 *
 * <ul>
 *   <li>jours disponibles d'un matériel = jours écoulés − jours d'immobilisation en maintenance ;
 *   <li>jours affectés = jours couverts par une affectation (transit compris) et non immobilisés ;
 *   <li>un jour couvert par plusieurs affectations n'est compté qu'une fois.
 * </ul>
 */
public final class CalculateurUtilisation {

  private CalculateurUtilisation() {}

  /**
   * @param periode période observée
   * @param aujourdhui date du jour (les jours futurs ne comptent pas)
   * @param materiels matériels du dénominateur
   * @param affectations affectations du numérateur
   * @param maintenances maintenances (immobilisations)
   * @return le taux en %, vide si aucun jour disponible (indicateur non défini)
   */
  public static Optional<BigDecimal> taux(
      Periode periode,
      LocalDate aujourdhui,
      Collection<UUID> materiels,
      Collection<AffectationVue> affectations,
      Collection<MaintenanceVue> maintenances) {
    Optional<Periode> ecoulee = periode.ecouleeAu(aujourdhui);
    if (ecoulee.isEmpty() || materiels.isEmpty()) {
      return Optional.empty();
    }
    Periode e = ecoulee.get();
    int n = (int) e.jours();
    Map<UUID, List<AffectationVue>> affParMateriel =
        affectations.stream()
            .filter(a -> a.materielId() != null)
            .collect(Collectors.groupingBy(AffectationVue::materielId));
    Map<UUID, List<MaintenanceVue>> maintParMateriel =
        maintenances.stream()
            .filter(m -> m.materielId() != null)
            .collect(Collectors.groupingBy(MaintenanceVue::materielId));

    long affectes = 0;
    long disponibles = 0;
    for (UUID materiel : Set.copyOf(materiels)) {
      BitSet dispo = new BitSet(n);
      dispo.set(0, n);
      for (MaintenanceVue m : maintParMateriel.getOrDefault(materiel, List.of())) {
        m.immobilisation(aujourdhui)
            .flatMap(i -> e.intersection(i.du(), i.au()))
            .ifPresent(i -> dispo.clear(index(e, i.du()), index(e, i.au()) + 1));
      }
      BitSet occupe = new BitSet(n);
      for (AffectationVue a : affParMateriel.getOrDefault(materiel, List.of())) {
        a.joursOccupes(aujourdhui)
            .flatMap(o -> e.intersection(o.du(), o.au()))
            .ifPresent(o -> occupe.set(index(e, o.du()), index(e, o.au()) + 1));
      }
      occupe.and(dispo);
      affectes += occupe.cardinality();
      disponibles += dispo.cardinality();
    }
    return Arrondis.tauxDefini(affectes, disponibles);
  }

  /**
   * Taux d'utilisation d'un périmètre :
   *
   * <ul>
   *   <li>sans restriction : tous les matériels non réformés (de la catégorie) au dénominateur ;
   *   <li>périmètre restreint à des chantiers : les matériels affectés à ces chantiers sur la
   *       période au dénominateur, leurs affectations à ces chantiers au numérateur.
   * </ul>
   *
   * @param periode période
   * @param aujourdhui date du jour
   * @param donnees données déjà restreintes au périmètre
   * @param perimetre périmètre
   * @param categorie filtre de catégorie
   * @param immobilisations maintenances de tous les chantiers : l'immobilisation d'un matériel
   *     compte quel que soit le chantier rattaché à l'intervention
   * @return le taux, vide s'il n'est pas défini
   */
  public static Optional<BigDecimal> tauxPerimetre(
      Periode periode,
      LocalDate aujourdhui,
      DonneesReporting donnees,
      Perimetre perimetre,
      Predicate<CategorieMateriel> categorie,
      Collection<MaintenanceVue> immobilisations) {
    Map<UUID, CategorieMateriel> categories =
        donnees.materiels().stream()
            .filter(m -> m.categorie() != null)
            .collect(Collectors.toMap(MaterielVue::id, MaterielVue::categorie, (a, b) -> a));
    List<AffectationVue> affectations =
        donnees.affectations().stream()
            .filter(
                a -> {
                  CategorieMateriel c =
                      a.categorie() != null ? a.categorie() : categories.get(a.materielId());
                  return c != null && categorie.test(c);
                })
            .toList();
    Set<UUID> materiels;
    if (perimetre.restreint()) {
      materiels =
          affectations.stream()
              .filter(a -> a.joursDans(periode, aujourdhui) > 0)
              .map(AffectationVue::materielId)
              .collect(Collectors.toSet());
    } else {
      materiels =
          donnees.materiels().stream()
              .filter(m -> !m.reforme() && m.categorie() != null && categorie.test(m.categorie()))
              .map(MaterielVue::id)
              .collect(Collectors.toSet());
    }
    return taux(periode, aujourdhui, materiels, affectations, immobilisations);
  }

  private static int index(Periode e, LocalDate jour) {
    return (int) ChronoUnit.DAYS.between(e.du(), jour);
  }
}
