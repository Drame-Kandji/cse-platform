package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.StatutAffectation;

/**
 * Vue d'une affectation matériel → chantier, base des jours d'utilisation et du coût
 * d'exploitation.
 *
 * @param id identifiant
 * @param materielId matériel
 * @param categorie catégorie du matériel
 * @param chantierId chantier
 * @param tarifJournalier tarif journalier au moment de l'affectation (FCFA)
 * @param besoinId besoin d'origine
 * @param dateDebut début
 * @param dateFinPrevue fin prévue
 * @param dateFinReelle fin réelle (libération)
 * @param statut statut
 * @param majLe instant du dernier événement appliqué
 */
public record AffectationVue(
    UUID id,
    UUID materielId,
    CategorieMateriel categorie,
    UUID chantierId,
    BigDecimal tarifJournalier,
    UUID besoinId,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    LocalDate dateFinReelle,
    StatutAffectation statut,
    Instant majLe) {

  public AffectationVue {
    Objects.requireNonNull(id, "id");
    tarifJournalier = tarifJournalier == null ? BigDecimal.ZERO : tarifJournalier;
  }

  /**
   * @param recu instantané reçu
   * @return l'instantané le plus récent
   */
  public AffectationVue fusionnerAvec(AffectationVue recu) {
    return Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
  }

  /**
   * Jours pendant lesquels le matériel est affecté (en transit ou reçu) : de la date de début à la
   * fin réelle ; sans fin réelle, jusqu'à aujourd'hui (ou la fin prévue si l'affectation est
   * terminée sans date de libération).
   *
   * @param aujourdhui date du jour (aucun jour futur n'est compté)
   * @return l'intervalle occupé, vide s'il commence dans le futur
   */
  public Optional<Periode> joursOccupes(LocalDate aujourdhui) {
    if (dateDebut == null) {
      return Optional.empty();
    }
    LocalDate fin;
    if (dateFinReelle != null) {
      fin = dateFinReelle;
    } else if (statut == StatutAffectation.TERMINEE && dateFinPrevue != null) {
      fin = dateFinPrevue;
    } else {
      fin = aujourdhui;
    }
    if (fin.isAfter(aujourdhui)) {
      fin = aujourdhui;
    }
    return dateDebut.isAfter(fin) ? Optional.empty() : Optional.of(new Periode(dateDebut, fin));
  }

  /**
   * @param periode période observée
   * @param aujourdhui date du jour
   * @return nombre de jours affectés dans la période
   */
  public long joursDans(Periode periode, LocalDate aujourdhui) {
    return joursOccupes(aujourdhui)
        .flatMap(o -> periode.intersection(o.du(), o.au()))
        .map(Periode::jours)
        .orElse(0L);
  }

  /**
   * @param periode période observée
   * @param aujourdhui date du jour
   * @return coût d'exploitation = jours affectés × tarif journalier
   */
  public BigDecimal coutDans(Periode periode, LocalDate aujourdhui) {
    return tarifJournalier.multiply(BigDecimal.valueOf(joursDans(periode, aujourdhui)));
  }
}
