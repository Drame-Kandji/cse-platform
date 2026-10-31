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
import sn.cse.reporting.domain.model.HistoriqueParc;
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
   * @param historique historique du parc : reconstitue les jours antérieurs au premier instantané
   *     enregistré ({@link HistoriqueParc#vide()} si l'indicateur n'est pas une disponibilité)
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
      HistoriqueParc historique,
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
    Disponibilite disponibilite =
        new Disponibilite(
            parJour,
            instantanes.stream().map(ParcInstantane::jour).min(LocalDate::compareTo).orElse(null),
            new ReconstitutionParc(historique, aujourdhui),
            perimetre,
            aujourdhui);
    List<Point> points = new ArrayList<>();
    for (Tranche t : tranches) {
      valeur(
              indicateur,
              t.periode(),
              perimetre,
              d,
              donnees.maintenances(),
              disponibilite,
              aujourdhui)
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
      Disponibilite disponibilite,
      LocalDate aujourdhui) {
    return switch (indicateur) {
      case TAUX_DISPONIBILITE -> disponibilite.a(p);
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
   * Disponibilité opérationnelle à la fin de la tranche (ou aujourd'hui si la tranche est en cours)
   * = (total − en maintenance − réformés) / (total − réformés), d'après le dernier instantané
   * enregistré à cette date (l'état est reporté tant qu'aucun nouvel instantané n'existe) ; avant
   * le premier instantané enregistré, l'instantané du jour est reconstitué à partir de l'historique
   * des affectations et maintenances.
   */
  private record Disponibilite(
      NavigableMap<LocalDate, List<ParcInstantane>> instantanes,
      LocalDate premierEnregistre,
      ReconstitutionParc reconstitution,
      Perimetre perimetre,
      LocalDate aujourdhui) {

    Optional<BigDecimal> a(Periode p) {
      LocalDate fin = p.au().isAfter(aujourdhui) ? aujourdhui : p.au();
      if (fin.isBefore(p.du())) {
        return Optional.empty();
      }
      List<ParcInstantane> lignes;
      if (premierEnregistre == null || fin.isBefore(premierEnregistre)) {
        lignes =
            reconstitution.au(fin).stream().filter(i -> perimetre.inclut(i.chantierId())).toList();
      } else {
        Map.Entry<LocalDate, List<ParcInstantane>> photo = instantanes.floorEntry(fin);
        lignes = photo == null ? List.of() : photo.getValue();
      }
      if (lignes.isEmpty()) {
        return Optional.empty();
      }
      long total = 0;
      long operationnels = 0;
      long reformes = 0;
      for (ParcInstantane i : lignes) {
        total += i.total();
        operationnels += i.operationnels();
        reformes += i.reformes();
      }
      return Arrondis.tauxDefini(operationnels, total - reformes);
    }
  }
}
