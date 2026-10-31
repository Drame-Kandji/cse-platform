package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.StatutMaintenance;
import sn.cse.reporting.domain.model.TypeMaintenance;

/**
 * Vue d'une intervention de maintenance : coût et jours d'immobilisation.
 *
 * @param id identifiant
 * @param numeroOt numéro d'ordre de travail
 * @param materielId matériel
 * @param chantierId chantier du matériel au moment de l'intervention
 * @param type type
 * @param statut statut
 * @param cout coût (FCFA), connu à la clôture
 * @param datePlanifiee date planifiée
 * @param dateDebut début effectif
 * @param dateFin fin effective
 * @param clotureeLe instant de l'événement de clôture
 * @param majLe instant du dernier événement appliqué
 */
public record MaintenanceVue(
    UUID id,
    String numeroOt,
    UUID materielId,
    UUID chantierId,
    TypeMaintenance type,
    StatutMaintenance statut,
    BigDecimal cout,
    LocalDate datePlanifiee,
    LocalDate dateDebut,
    LocalDate dateFin,
    Instant clotureeLe,
    Instant majLe) {

  public MaintenanceVue {
    Objects.requireNonNull(id, "id");
  }

  /**
   * @param recu instantané reçu
   * @return l'instantané le plus récent, en conservant l'instant de clôture connu
   */
  public MaintenanceVue fusionnerAvec(MaintenanceVue recu) {
    MaintenanceVue retenu = Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
    Instant cloture = Horodatage.min(clotureeLe, recu.clotureeLe);
    return new MaintenanceVue(
        id,
        retenu.numeroOt,
        retenu.materielId,
        retenu.chantierId,
        retenu.type,
        retenu.statut,
        retenu.cout,
        retenu.datePlanifiee,
        retenu.dateDebut,
        retenu.dateFin,
        cloture,
        retenu.majLe);
  }

  /**
   * @return date d'imputation du coût : fin effective, à défaut début puis date planifiée ; vide si
   *     l'intervention n'est pas clôturée ou n'a pas de coût
   */
  public Optional<LocalDate> dateCout() {
    if (statut != StatutMaintenance.CLOTUREE || cout == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        Horodatage.sinon(dateFin, Horodatage.sinon(dateDebut, datePlanifiee)));
  }

  /**
   * @param periode période
   * @return coût imputé à la période (zéro hors période)
   */
  public BigDecimal coutDans(Periode periode) {
    return dateCout().filter(periode::contient).map(d -> cout).orElse(BigDecimal.ZERO);
  }

  /**
   * @param aujourdhui date du jour
   * @return jours d'immobilisation (début → fin, ou aujourd'hui si en cours)
   */
  public Optional<Periode> immobilisation(LocalDate aujourdhui) {
    if (dateDebut == null || statut == StatutMaintenance.PLANIFIEE || statut == null) {
      return Optional.empty();
    }
    LocalDate fin = dateFin != null ? dateFin : aujourdhui;
    if (statut == StatutMaintenance.CLOTUREE && dateFin == null) {
      fin = dateDebut;
    }
    return dateDebut.isAfter(fin) ? Optional.empty() : Optional.of(new Periode(dateDebut, fin));
  }

  /**
   * @return vrai si l'intervention est en cours
   */
  public boolean enCours() {
    return statut == StatutMaintenance.EN_COURS;
  }
}
