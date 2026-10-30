package sn.cse.suivi.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/** Panne signalée sur un matériel. */
public final class Panne {

  private final UUID id;
  private final UUID materielId;
  private final UUID chantierId;
  private final GravitePanne gravite;
  private final String description;
  private StatutPanne statut;
  private final UUID signaleParId;
  private final String signaleParNom;
  private final Instant dateSignalement;
  private UUID maintenanceId;
  private final Long version;

  /**
   * État persistant d'une panne.
   *
   * @param id identifiant
   * @param materielId matériel
   * @param chantierId chantier où se trouvait le matériel
   * @param gravite gravité
   * @param description symptômes
   * @param statut statut
   * @param signaleParId auteur du signalement
   * @param signaleParNom nom de l'auteur
   * @param dateSignalement instant du signalement
   * @param maintenanceId ordre de travail de prise en charge
   * @param version version de verrou optimiste
   */
  public record Snapshot(
      UUID id,
      UUID materielId,
      UUID chantierId,
      GravitePanne gravite,
      String description,
      StatutPanne statut,
      UUID signaleParId,
      String signaleParNom,
      Instant dateSignalement,
      UUID maintenanceId,
      Long version) {}

  private Panne(Snapshot s) {
    this.id = Objects.requireNonNull(s.id(), "id");
    this.materielId = Objects.requireNonNull(s.materielId(), "materielId");
    this.chantierId = s.chantierId();
    this.gravite = Objects.requireNonNull(s.gravite(), "gravite");
    this.description = s.description();
    this.statut = Objects.requireNonNull(s.statut(), "statut");
    this.signaleParId = s.signaleParId();
    this.signaleParNom = s.signaleParNom();
    this.dateSignalement = Objects.requireNonNull(s.dateSignalement(), "dateSignalement");
    this.maintenanceId = s.maintenanceId();
    this.version = s.version();
  }

  /**
   * Signale une panne sur le chantier courant du matériel.
   *
   * @param materiel matériel en panne
   * @param gravite gravité
   * @param description symptômes
   * @param auteurId auteur
   * @param auteurNom nom de l'auteur
   * @param instant moment du signalement
   * @return la panne
   */
  public static Panne signaler(
      Materiel materiel,
      GravitePanne gravite,
      String description,
      UUID auteurId,
      String auteurNom,
      Instant instant) {
    if (materiel.getEtat() == EtatMateriel.REFORME) {
      throw SuiviErreurs.materielReforme(materiel.getCode());
    }
    return new Panne(
        new Snapshot(
            UUID.randomUUID(),
            materiel.getId(),
            materiel.getChantierActuelId(),
            gravite,
            description,
            StatutPanne.SIGNALEE,
            auteurId,
            auteurNom,
            instant,
            null,
            null));
  }

  /**
   * @param snapshot état persistant
   * @return la panne reconstituée
   */
  public static Panne reconstituer(Snapshot snapshot) {
    return new Panne(snapshot);
  }

  /**
   * Rattache la panne à l'ordre de travail qui la traite.
   *
   * @param maintenance ordre de travail
   * @param materielDeLaMaintenance matériel de l'ordre de travail (doit être le même)
   */
  public void prendreEnCharge(UUID maintenance, UUID materielDeLaMaintenance) {
    if (!materielId.equals(materielDeLaMaintenance)) {
      throw SuiviErreurs.regle(
          "PANNE_INCOHERENTE", "La panne ne concerne pas le matériel de l'ordre de travail.");
    }
    statut.verifierTransition(StatutPanne.PRISE_EN_CHARGE);
    this.statut = StatutPanne.PRISE_EN_CHARGE;
    this.maintenanceId = maintenance;
  }

  /** Marque la panne résolue (clôture de l'ordre de travail). */
  public void resoudre() {
    statut.verifierTransition(StatutPanne.RESOLUE);
    this.statut = StatutPanne.RESOLUE;
  }

  /**
   * @return l'état persistant
   */
  public Snapshot snapshot() {
    return new Snapshot(
        id,
        materielId,
        chantierId,
        gravite,
        description,
        statut,
        signaleParId,
        signaleParNom,
        dateSignalement,
        maintenanceId,
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

  public GravitePanne getGravite() {
    return gravite;
  }

  public String getDescription() {
    return description;
  }

  public StatutPanne getStatut() {
    return statut;
  }

  public UUID getSignaleParId() {
    return signaleParId;
  }

  public String getSignaleParNom() {
    return signaleParNom;
  }
}
