package sn.cse.suivi.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/** Affectation d'un matériel à un chantier sur une période. */
public final class Affectation {

  private final UUID id;
  private final UUID materielId;
  private final UUID chantierId;
  private final UUID besoinId;
  private final Periode periodePrevue;
  private LocalDate dateFinReelle;
  private StatutAffectation statut;
  private Instant dateReception;
  private String observationsReception;
  private String commentaireLiberation;
  private final UUID fournisseurId;
  private final UUID contratId;
  private boolean alerteFinEnvoyee;
  private final Long version;

  /**
   * État persistant d'une affectation.
   *
   * @param id identifiant
   * @param materielId matériel
   * @param chantierId chantier
   * @param besoinId besoin d'origine (optionnel)
   * @param periodePrevue période prévue
   * @param dateFinReelle date de fin effective
   * @param statut statut
   * @param dateReception instant de la confirmation de réception
   * @param observationsReception observations du chef de chantier
   * @param commentaireLiberation commentaire de libération
   * @param fournisseurId fournisseur (matériel livré)
   * @param contratId contrat (matériel livré)
   * @param alerteFinEnvoyee alerte de fin prochaine déjà émise
   * @param version version de verrou optimiste
   */
  public record Snapshot(
      UUID id,
      UUID materielId,
      UUID chantierId,
      UUID besoinId,
      Periode periodePrevue,
      LocalDate dateFinReelle,
      StatutAffectation statut,
      Instant dateReception,
      String observationsReception,
      String commentaireLiberation,
      UUID fournisseurId,
      UUID contratId,
      boolean alerteFinEnvoyee,
      Long version) {}

  private Affectation(Snapshot s) {
    this.id = Objects.requireNonNull(s.id(), "id");
    this.materielId = Objects.requireNonNull(s.materielId(), "materielId");
    this.chantierId = Objects.requireNonNull(s.chantierId(), "chantierId");
    this.besoinId = s.besoinId();
    this.periodePrevue = Objects.requireNonNull(s.periodePrevue(), "periodePrevue");
    this.dateFinReelle = s.dateFinReelle();
    this.statut = Objects.requireNonNull(s.statut(), "statut");
    this.dateReception = s.dateReception();
    this.observationsReception = s.observationsReception();
    this.commentaireLiberation = s.commentaireLiberation();
    this.fournisseurId = s.fournisseurId();
    this.contratId = s.contratId();
    this.alerteFinEnvoyee = s.alerteFinEnvoyee();
    this.version = s.version();
  }

  /**
   * Crée l'affectation en attente de réception par le chef de chantier. Les contrôles d'éligibilité
   * sont portés par {@link sn.cse.suivi.domain.service.PolitiqueAffectation}.
   *
   * @param materiel matériel affecté
   * @param chantier chantier de destination
   * @param besoinId besoin à l'origine (optionnel)
   * @param periode période prévue
   * @return l'affectation
   */
  public static Affectation creer(
      Materiel materiel, Chantier chantier, UUID besoinId, Periode periode) {
    return new Affectation(
        new Snapshot(
            UUID.randomUUID(),
            materiel.getId(),
            chantier.getId(),
            besoinId,
            periode,
            null,
            StatutAffectation.EN_ATTENTE_RECEPTION,
            null,
            null,
            null,
            materiel.getFournisseurId(),
            materiel.getContratId(),
            false,
            null));
  }

  /**
   * @param snapshot état persistant
   * @return l'affectation reconstituée
   */
  public static Affectation reconstituer(Snapshot snapshot) {
    return new Affectation(snapshot);
  }

  /**
   * Le chef de chantier confirme la réception physique du matériel.
   *
   * @param instant moment de la confirmation
   * @param observations observations éventuelles (état à la livraison…)
   */
  public void confirmerReception(Instant instant, String observations) {
    if (statut != StatutAffectation.EN_ATTENTE_RECEPTION) {
      throw SuiviErreurs.transitionInvalide("l'affectation", statut, StatutAffectation.EN_COURS);
    }
    this.statut = StatutAffectation.EN_COURS;
    this.dateReception = instant;
    this.observationsReception = observations;
  }

  /**
   * Termine l'affectation (libération du matériel).
   *
   * @param dateFin date de fin effective
   * @param commentaire commentaire éventuel
   */
  public void terminer(LocalDate dateFin, String commentaire) {
    if (!statut.estActif()) {
      throw SuiviErreurs.transitionInvalide("l'affectation", statut, StatutAffectation.TERMINEE);
    }
    new Periode(periodePrevue.debut(), dateFin);
    this.statut = StatutAffectation.TERMINEE;
    this.dateFinReelle = dateFin;
    this.commentaireLiberation = commentaire;
  }

  /**
   * @return vrai si l'affectation mobilise encore le matériel
   */
  public boolean estActive() {
    return statut.estActif();
  }

  /**
   * @param periode période candidate
   * @return vrai si l'affectation est active et sa période prévue chevauche {@code periode}
   */
  public boolean bloque(Periode periode) {
    return estActive() && periodePrevue.chevauche(periode);
  }

  /**
   * @return période effective (fin réelle si terminée, sinon fin prévue)
   */
  public Periode periodeEffective() {
    return dateFinReelle == null
        ? periodePrevue
        : new Periode(periodePrevue.debut(), dateFinReelle);
  }

  /**
   * @param aujourdhui date du jour
   * @param joursAvant fenêtre d'alerte (J-n)
   * @return vrai si l'alerte de fin prochaine doit être émise
   */
  public boolean doitAlerterFinProchaine(LocalDate aujourdhui, int joursAvant) {
    LocalDate fin = periodePrevue.fin();
    return estActive()
        && !alerteFinEnvoyee
        && !fin.isBefore(aujourdhui)
        && !fin.isAfter(aujourdhui.plusDays(joursAvant));
  }

  /** Mémorise l'émission de l'alerte de fin prochaine. */
  public void marquerAlerteFinEnvoyee() {
    this.alerteFinEnvoyee = true;
  }

  /**
   * @return l'état persistant
   */
  public Snapshot snapshot() {
    return new Snapshot(
        id,
        materielId,
        chantierId,
        besoinId,
        periodePrevue,
        dateFinReelle,
        statut,
        dateReception,
        observationsReception,
        commentaireLiberation,
        fournisseurId,
        contratId,
        alerteFinEnvoyee,
        version);
  }

  public UUID getId() {
    return id;
  }

  public UUID getMaterielId() {
    return materielId;
  }

  public UUID getChantierId() {
    return chantierId;
  }

  public UUID getBesoinId() {
    return besoinId;
  }

  public LocalDate getDateDebut() {
    return periodePrevue.debut();
  }

  public LocalDate getDateFinPrevue() {
    return periodePrevue.fin();
  }

  public LocalDate getDateFinReelle() {
    return dateFinReelle;
  }

  public StatutAffectation getStatut() {
    return statut;
  }

  public Instant getDateReception() {
    return dateReception;
  }

  public String getObservationsReception() {
    return observationsReception;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public UUID getContratId() {
    return contratId;
  }

  public Long getVersion() {
    return version;
  }
}
