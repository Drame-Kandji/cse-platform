package sn.cse.suivi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/** Ordre de travail (OT) de maintenance d'un matériel. */
public final class Maintenance {

  private final UUID id;
  private final String numeroOt;
  private final UUID materielId;
  private final UUID chantierId;
  private final TypeMaintenance type;
  private StatutMaintenance statut;
  private final String description;
  private final LocalDate datePlanifiee;
  private LocalDate dateDebut;
  private LocalDate dateFin;
  private final String technicien;
  private BigDecimal cout;
  private Long heuresCompteur;
  private String observations;
  private final UUID panneId;
  private final Long version;

  /**
   * État persistant d'un ordre de travail.
   *
   * @param id identifiant
   * @param numeroOt numéro lisible ({@code OT-2026-0001})
   * @param materielId matériel concerné
   * @param chantierId chantier du matériel lors de la planification
   * @param type nature
   * @param statut statut
   * @param description travaux prévus
   * @param datePlanifiee date prévue
   * @param dateDebut début effectif
   * @param dateFin fin effective
   * @param technicien intervenant
   * @param cout coût total (FCFA)
   * @param heuresCompteur compteur relevé à la clôture
   * @param observations compte rendu
   * @param panneId panne à l'origine (maintenance corrective)
   * @param version version de verrou optimiste
   */
  public record Snapshot(
      UUID id,
      String numeroOt,
      UUID materielId,
      UUID chantierId,
      TypeMaintenance type,
      StatutMaintenance statut,
      String description,
      LocalDate datePlanifiee,
      LocalDate dateDebut,
      LocalDate dateFin,
      String technicien,
      BigDecimal cout,
      Long heuresCompteur,
      String observations,
      UUID panneId,
      Long version) {}

  /**
   * Demande de planification.
   *
   * @param type nature
   * @param description travaux prévus
   * @param datePlanifiee date prévue
   * @param technicien intervenant (optionnel)
   * @param panneId panne à l'origine (optionnel)
   */
  public record Planification(
      TypeMaintenance type,
      String description,
      LocalDate datePlanifiee,
      String technicien,
      UUID panneId) {}

  private Maintenance(Snapshot s) {
    this.id = Objects.requireNonNull(s.id(), "id");
    this.numeroOt = Objects.requireNonNull(s.numeroOt(), "numeroOt");
    this.materielId = Objects.requireNonNull(s.materielId(), "materielId");
    this.chantierId = s.chantierId();
    this.type = Objects.requireNonNull(s.type(), "type");
    this.statut = Objects.requireNonNull(s.statut(), "statut");
    this.description = s.description();
    this.datePlanifiee = Objects.requireNonNull(s.datePlanifiee(), "datePlanifiee");
    this.dateDebut = s.dateDebut();
    this.dateFin = s.dateFin();
    this.technicien = s.technicien();
    this.cout = s.cout();
    this.heuresCompteur = s.heuresCompteur();
    this.observations = s.observations();
    this.panneId = s.panneId();
    this.version = s.version();
  }

  /**
   * Planifie un ordre de travail ({@code PLANIFIEE}). Un matériel réformé n'est plus entretenu.
   *
   * @param numeroOt numéro attribué
   * @param materiel matériel concerné
   * @param demande planification
   * @return l'ordre de travail
   */
  public static Maintenance planifier(String numeroOt, Materiel materiel, Planification demande) {
    if (materiel.getEtat() == EtatMateriel.REFORME) {
      throw SuiviErreurs.materielReforme(materiel.getCode());
    }
    return new Maintenance(
        new Snapshot(
            UUID.randomUUID(),
            numeroOt,
            materiel.getId(),
            materiel.getChantierActuelId(),
            demande.type(),
            StatutMaintenance.PLANIFIEE,
            demande.description(),
            demande.datePlanifiee(),
            null,
            null,
            demande.technicien(),
            null,
            null,
            null,
            demande.panneId(),
            null));
  }

  /**
   * @param snapshot état persistant
   * @return l'ordre de travail reconstitué
   */
  public static Maintenance reconstituer(Snapshot snapshot) {
    return new Maintenance(snapshot);
  }

  /**
   * Démarre l'intervention.
   *
   * @param date date de début
   */
  public void demarrer(LocalDate date) {
    statut.verifierTransition(StatutMaintenance.EN_COURS);
    this.statut = StatutMaintenance.EN_COURS;
    this.dateDebut = date;
  }

  /**
   * Clôture l'intervention.
   *
   * @param date date de fin
   * @param coutTotal coût (FCFA, ≥ 0)
   * @param compteRendu observations
   * @param compteur compteur horaire relevé (optionnel)
   */
  public void cloturer(LocalDate date, BigDecimal coutTotal, String compteRendu, Long compteur) {
    statut.verifierTransition(StatutMaintenance.CLOTUREE);
    if (coutTotal == null || coutTotal.signum() < 0) {
      throw SuiviErreurs.regle(
          "MONTANT_INVALIDE", "Le coût de l'intervention est obligatoire et doit être positif.");
    }
    this.statut = StatutMaintenance.CLOTUREE;
    this.dateFin = date;
    this.cout = coutTotal;
    this.observations = compteRendu;
    this.heuresCompteur = compteur;
  }

  /**
   * @return l'état persistant
   */
  public Snapshot snapshot() {
    return new Snapshot(
        id,
        numeroOt,
        materielId,
        chantierId,
        type,
        statut,
        description,
        datePlanifiee,
        dateDebut,
        dateFin,
        technicien,
        cout,
        heuresCompteur,
        observations,
        panneId,
        version);
  }

  public UUID getId() {
    return id;
  }

  public String getNumeroOt() {
    return numeroOt;
  }

  public UUID getMaterielId() {
    return materielId;
  }

  public UUID getChantierId() {
    return chantierId;
  }

  public TypeMaintenance getType() {
    return type;
  }

  public StatutMaintenance getStatut() {
    return statut;
  }

  public LocalDate getDatePlanifiee() {
    return datePlanifiee;
  }

  public LocalDate getDateDebut() {
    return dateDebut;
  }

  public LocalDate getDateFin() {
    return dateFin;
  }

  public BigDecimal getCout() {
    return cout;
  }

  public UUID getPanneId() {
    return panneId;
  }

  public Long getVersion() {
    return version;
  }
}
