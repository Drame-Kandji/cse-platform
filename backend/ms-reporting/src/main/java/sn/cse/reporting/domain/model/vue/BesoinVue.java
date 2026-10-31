package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.StatutBesoin;

/**
 * Vue d'un besoin matériel.
 *
 * <p>Horodatages : {@code soumisLe} = instant de {@code BESOIN_SOUMIS} ; {@code decideLe} = premier
 * instant d'une décision du RM (validé, rejeté, affecté, en achat) ; {@code premierVuLe} = premier
 * événement reçu (repli si la soumission n'a pas été reçue).
 *
 * @param id identifiant
 * @param reference référence lisible
 * @param chantierId chantier demandeur
 * @param chantierNom nom du chantier
 * @param categorie catégorie demandée
 * @param quantite quantité
 * @param priorite priorité
 * @param statut statut courant
 * @param dateBesoin date à laquelle le matériel est nécessaire
 * @param soumisLe instant de soumission
 * @param decideLe instant de la décision
 * @param premierVuLe instant du premier événement reçu
 * @param majLe instant du dernier événement appliqué
 */
public record BesoinVue(
    UUID id,
    String reference,
    UUID chantierId,
    String chantierNom,
    CategorieMateriel categorie,
    int quantite,
    Priorite priorite,
    StatutBesoin statut,
    LocalDate dateBesoin,
    Instant soumisLe,
    Instant decideLe,
    Instant premierVuLe,
    Instant majLe) {

  private static final BigDecimal SECONDES_PAR_HEURE = BigDecimal.valueOf(3600);

  public BesoinVue {
    Objects.requireNonNull(id, "id");
  }

  /**
   * Fusion : l'instantané le plus récent fixe statut et attributs ; les horodatages de soumission
   * et de décision conservent leur première valeur connue.
   *
   * @param recu vue construite à partir de l'événement reçu
   * @return la vue fusionnée
   */
  public BesoinVue fusionnerAvec(BesoinVue recu) {
    BesoinVue r = Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
    return new BesoinVue(
        id,
        r.reference,
        r.chantierId,
        r.chantierNom,
        r.categorie,
        r.quantite,
        r.priorite,
        r.statut,
        r.dateBesoin,
        Horodatage.min(soumisLe, recu.soumisLe),
        Horodatage.min(decideLe, recu.decideLe),
        Horodatage.min(premierVuLe, recu.premierVuLe),
        Horodatage.max(majLe, recu.majLe));
  }

  /**
   * Jour de soumission retenu pour les séries : jour de {@code BESOIN_SOUMIS} (à défaut, du premier
   * événement reçu), ramené à la date du besoin si celle-ci est antérieure. Ce second cas ne se
   * produit que pour des besoins historiques saisis a posteriori (données de démonstration datées
   * sur 9 mois mais publiées au démarrage) : un besoin réel est toujours soumis avant d'être
   * nécessaire.
   *
   * @return le jour de soumission, s'il est connu
   */
  public Optional<LocalDate> dateSoumission() {
    LocalDate jour = Horodatage.jour(Horodatage.sinon(soumisLe, premierVuLe));
    return Optional.ofNullable(Horodatage.min(jour, dateBesoin));
  }

  /**
   * @return délai soumission → décision en heures (une décimale), si les deux sont connus
   */
  public Optional<BigDecimal> delaiTraitementHeures() {
    if (soumisLe == null || decideLe == null || decideLe.isBefore(soumisLe)) {
      return Optional.empty();
    }
    long secondes = Duration.between(soumisLe, decideLe).toSeconds();
    return Optional.of(
        BigDecimal.valueOf(secondes).divide(SECONDES_PAR_HEURE, 1, RoundingMode.HALF_UP));
  }
}
