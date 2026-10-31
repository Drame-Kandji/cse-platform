package sn.cse.reporting.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import sn.cse.common.exception.InvalidRequestException;

/**
 * Intervalle de jours calendaires, bornes incluses.
 *
 * @param du premier jour inclus
 * @param au dernier jour inclus
 */
public record Periode(LocalDate du, LocalDate au) {

  public Periode {
    if (du == null || au == null) {
      throw new InvalidRequestException("PERIODE_INVALIDE", "Les bornes de période sont requises.");
    }
    if (du.isAfter(au)) {
      throw new InvalidRequestException(
          "PERIODE_INVALIDE", "La date de début doit précéder ou égaler la date de fin.");
    }
  }

  /**
   * Période d'un tableau de bord : par défaut les {@code defautJours} derniers jours, jour courant
   * inclus ; une borne absente est déduite de l'autre.
   *
   * @param du début demandé (facultatif)
   * @param au fin demandée (facultatif, défaut : aujourd'hui)
   * @param aujourdhui date du jour
   * @param defautJours longueur par défaut
   * @param maxJours longueur maximale
   * @return la période
   * @throws InvalidRequestException si la période est inversée ou trop longue
   */
  public static Periode resoudre(
      LocalDate du, LocalDate au, LocalDate aujourdhui, int defautJours, int maxJours) {
    LocalDate fin = au != null ? au : aujourdhui;
    LocalDate debut = du != null ? du : fin.minusDays(defautJours - 1L);
    Periode p = new Periode(debut, fin);
    if (p.jours() > maxJours) {
      throw new InvalidRequestException(
          "PERIODE_TROP_LONGUE", "La période ne doit pas dépasser " + maxJours + " jours.");
    }
    return p;
  }

  /**
   * @return nombre de jours de la période (bornes incluses)
   */
  public long jours() {
    return ChronoUnit.DAYS.between(du, au) + 1;
  }

  /**
   * @param date date testée
   * @return vrai si la date appartient à la période
   */
  public boolean contient(LocalDate date) {
    return date != null && !date.isBefore(du) && !date.isAfter(au);
  }

  /**
   * @param debut début inclus de l'autre intervalle (null = sans borne)
   * @param fin fin incluse de l'autre intervalle (null = sans borne)
   * @return l'intersection, si elle n'est pas vide
   */
  public Optional<Periode> intersection(LocalDate debut, LocalDate fin) {
    LocalDate d = debut == null || debut.isBefore(du) ? du : debut;
    LocalDate f = fin == null || fin.isAfter(au) ? au : fin;
    return d.isAfter(f) ? Optional.empty() : Optional.of(new Periode(d, f));
  }

  /**
   * Partie écoulée de la période : les jours postérieurs à {@code aujourdhui} ne peuvent pas encore
   * avoir été utilisés.
   *
   * @param aujourdhui date du jour
   * @return la période tronquée, vide si elle commence dans le futur
   */
  public Optional<Periode> ecouleeAu(LocalDate aujourdhui) {
    return intersection(null, aujourdhui);
  }

  /**
   * @param granularite pas de découpage
   * @return les tranches successives, la première et la dernière étant tronquées aux bornes
   */
  public List<Tranche> decouper(Granularite granularite) {
    List<Tranche> tranches = new ArrayList<>();
    LocalDate debut = granularite.debut(du);
    while (!debut.isAfter(au)) {
      LocalDate suivant = granularite.suivant(debut);
      LocalDate d = debut.isBefore(du) ? du : debut;
      LocalDate f = suivant.minusDays(1).isAfter(au) ? au : suivant.minusDays(1);
      tranches.add(new Tranche(granularite.libelle(debut), new Periode(d, f)));
      debut = suivant;
    }
    return tranches;
  }

  /**
   * Tranche d'une série temporelle.
   *
   * @param libelle libellé ({@code 2026-07}, {@code 2026-07-14} ou {@code 2026-W29})
   * @param periode jours couverts
   */
  public record Tranche(String libelle, Periode periode) {}
}
