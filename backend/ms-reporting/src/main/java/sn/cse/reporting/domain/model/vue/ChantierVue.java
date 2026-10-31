package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.StatutChantier;

/**
 * Vue d'un chantier (CHANTIER_CREE / CHANTIER_MODIFIE). Une référence incomplète (nom seul) peut
 * être créée par un événement d'un autre contexte arrivé avant l'événement du chantier.
 *
 * @param id identifiant
 * @param code code ({@code CH-DKR-01})
 * @param nom nom
 * @param region région
 * @param statut statut
 * @param budget budget alloué (FCFA)
 * @param montantMarche montant du marché (recette attendue, FCFA)
 * @param chefChantierId chef de chantier
 * @param dateDebut date de début
 * @param dateFinPrevue date de fin prévue
 * @param complet vrai si la vue provient d'un événement du chantier lui-même
 * @param majLe instant du dernier événement appliqué
 */
public record ChantierVue(
    UUID id,
    String code,
    String nom,
    String region,
    StatutChantier statut,
    BigDecimal budget,
    BigDecimal montantMarche,
    UUID chefChantierId,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    boolean complet,
    Instant majLe) {

  public ChantierVue {
    Objects.requireNonNull(id, "id");
    budget = budget == null ? BigDecimal.ZERO : budget;
    montantMarche = montantMarche == null ? BigDecimal.ZERO : montantMarche;
  }

  /**
   * @param id identifiant du chantier
   * @param nom nom connu par l'événement d'un autre contexte
   * @param quand instant de cet événement
   * @return une référence incomplète
   */
  public static ChantierVue reference(UUID id, String nom, Instant quand) {
    return new ChantierVue(id, null, nom, null, null, null, null, null, null, null, false, quand);
  }

  /**
   * Fusion défensive : un instantané complet plus récent remplace la vue ; une référence ne fait
   * que compléter le nom manquant.
   *
   * @param recu vue construite à partir de l'événement reçu
   * @return la vue fusionnée
   */
  public ChantierVue fusionnerAvec(ChantierVue recu) {
    if (!recu.complet) {
      return nom != null || recu.nom == null
          ? this
          : new ChantierVue(
              id,
              code,
              recu.nom,
              region,
              statut,
              budget,
              montantMarche,
              chefChantierId,
              dateDebut,
              dateFinPrevue,
              complet,
              majLe);
    }
    return !complet || Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
  }

  /**
   * Un chantier est actif sur une période s'il a commencé avant la fin de la période et n'était pas
   * terminé avant son début (un chantier en cours est toujours actif).
   *
   * @param periode période
   * @return vrai si le chantier est actif sur la période
   */
  public boolean actifSur(Periode periode) {
    boolean commence = dateDebut == null || !dateDebut.isAfter(periode.au());
    boolean pasFini =
        statut != StatutChantier.TERMINE
            || dateFinPrevue == null
            || !dateFinPrevue.isBefore(periode.du());
    return commence && pasFini;
  }

  /**
   * Intervalle sur lequel le montant du marché est réparti linéairement.
   *
   * @param dureeDefautJours durée retenue sans date de fin prévue
   * @return l'intervalle, vide si la date de début est inconnue
   */
  public Optional<Periode> intervalleMarche(int dureeDefautJours) {
    if (dateDebut == null) {
      return Optional.empty();
    }
    LocalDate fin =
        dateFinPrevue == null ? dateDebut.plusDays(dureeDefautJours - 1L) : dateFinPrevue;
    return Optional.of(new Periode(dateDebut, fin.isBefore(dateDebut) ? dateDebut : fin));
  }

  /**
   * @return nom affichable (code ou identifiant à défaut)
   */
  public String nomAffichable() {
    if (nom != null) {
      return nom;
    }
    return code != null ? code : id.toString();
  }
}
