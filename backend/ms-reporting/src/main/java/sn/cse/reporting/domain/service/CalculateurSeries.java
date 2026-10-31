package sn.cse.reporting.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.Granularite;
import sn.cse.reporting.domain.model.Indicateur;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Periode.Tranche;
import sn.cse.reporting.domain.model.Point;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/**
 * Séries temporelles. Pour un cumul (montant, coût, nombre), chaque tranche produit un point
 * (éventuellement 0) ; pour un taux ou une moyenne, une tranche sans donnée ne produit pas de
 * point.
 */
public class CalculateurSeries {

  private final ParametresReporting parametres;

  /**
   * @param parametres paramètres métier (nombre maximal de points)
   */
  public CalculateurSeries(ParametresReporting parametres) {
    this.parametres = parametres;
  }

  /**
   * @param indicateur indicateur
   * @param granularite pas
   * @param periode période
   * @param perimetre périmètre
   * @param donnees vues chargées
   * @param instantanes instantanés quotidiens du parc (jusqu'à la fin de la période, y compris le
   *     dernier antérieur à son début)
   * @param aujourdhui date du jour
   * @return les points, dans l'ordre chronologique
   * @throws InvalidRequestException si la série dépasse le nombre maximal de points
   */
  public List<Point> calculer(
      Indicateur indicateur,
      Granularite granularite,
      Periode periode,
      Perimetre perimetre,
      DonneesReporting donnees,
      List<ParcInstantane> instantanes,
      LocalDate aujourdhui) {
    List<Tranche> tranches = periode.decouper(granularite);
    if (tranches.size() > parametres.maxPointsSerie()) {
      throw new InvalidRequestException(
          "SERIE_TROP_LONGUE",
          "La série demandée compte "
              + tranches.size()
              + " points (maximum "
              + parametres.maxPointsSerie()
              + ") : choisir une granularité plus large ou une période plus courte.");
    }
    DonneesReporting d = donnees.restreindre(perimetre);
    NavigableMap<LocalDate, List<ParcInstantane>> parJour =
        instantanes.stream()
            .filter(i -> perimetre.inclut(i.chantierId()))
            .collect(
                Collectors.groupingBy(ParcInstantane::jour, TreeMap::new, Collectors.toList()));
    List<Point> points = new ArrayList<>();
    for (Tranche t : tranches) {
      valeur(indicateur, t.periode(), perimetre, d, donnees.maintenances(), parJour, aujourdhui)
          .ifPresent(v -> points.add(new Point(t.libelle(), v)));
    }
    return points;
  }

  private static Optional<BigDecimal> valeur(
      Indicateur indicateur,
      Periode p,
      Perimetre perimetre,
      DonneesReporting d,
      List<MaintenanceVue> immobilisations,
      NavigableMap<LocalDate, List<ParcInstantane>> instantanes,
      LocalDate aujourdhui) {
    return switch (indicateur) {
      case TAUX_DISPONIBILITE -> disponibilite(p, instantanes, aujourdhui);
      case TAUX_UTILISATION ->
          CalculateurUtilisation.tauxPerimetre(
              p, aujourdhui, d, perimetre, c -> true, immobilisations);
      case MONTANT_ENGAGE -> Optional.of(Arrondis.montant(CalculateurTableau.montantEngage(p, d)));
      case COUT_MAINTENANCE ->
          Optional.of(Arrondis.montant(CalculateurTableau.coutMaintenance(p, d)));
      case BESOINS_SOUMIS ->
          Optional.of(BigDecimal.valueOf(CalculateurTableau.besoinsSoumis(p, d).size()));
      case DELAI_APPROVISIONNEMENT ->
          Arrondis.moyenne(CalculateurTableau.delaisApprovisionnement(p, d));
    };
  }

  /**
   * Disponibilité à la fin de la tranche (ou aujourd'hui si la tranche est en cours), d'après le
   * dernier instantané connu à cette date : l'état du parc est reporté tant qu'aucun nouvel
   * instantané n'existe.
   */
  private static Optional<BigDecimal> disponibilite(
      Periode p, NavigableMap<LocalDate, List<ParcInstantane>> instantanes, LocalDate aujourdhui) {
    LocalDate fin = p.au().isAfter(aujourdhui) ? aujourdhui : p.au();
    if (fin.isBefore(p.du())) {
      return Optional.empty();
    }
    Map.Entry<LocalDate, List<ParcInstantane>> photo = instantanes.floorEntry(fin);
    if (photo == null) {
      return Optional.empty();
    }
    long total = 0;
    long disponibles = 0;
    long reformes = 0;
    for (ParcInstantane i : photo.getValue()) {
      total += i.total();
      disponibles += i.disponibles();
      reformes += i.reformes();
    }
    return Arrondis.tauxDefini(disponibles, total - reformes);
  }
}
