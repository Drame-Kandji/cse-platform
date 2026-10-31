package sn.cse.suivi.infrastructure.messaging;

import org.springframework.stereotype.Component;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.events.payload.ChantierPayload;
import sn.cse.events.payload.MaintenancePayload;
import sn.cse.events.payload.MaterielPayload;
import sn.cse.events.payload.PannePayload;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Panne;

/**
 * Publication des événements du suivi sur {@code cse.suivi.events} via l'outbox transactionnelle
 * (une entrée d'audit est ajoutée automatiquement pour chaque événement).
 */
@Component
public class OutboxSuiviEventPublisher implements SuiviEventPublisher {

  static final String CHANTIER = "Chantier";
  static final String MATERIEL = "Materiel";
  static final String AFFECTATION = "Affectation";
  static final String MAINTENANCE = "Maintenance";
  static final String PANNE = "Panne";

  private final OutboxWriter outbox;

  /**
   * @param outbox écrivain d'outbox
   */
  public OutboxSuiviEventPublisher(OutboxWriter outbox) {
    this.outbox = outbox;
  }

  @Override
  public void chantier(String type, Chantier c) {
    outbox.publier(
        Topics.SUIVI,
        type,
        CHANTIER,
        c.getId(),
        new ChantierPayload(
            c.getId(),
            c.getCode(),
            c.getNom(),
            c.getRegion(),
            c.getStatut().name(),
            c.getBudget(),
            c.getMontantMarche(),
            c.getChefChantierId(),
            c.getDateDebut(),
            c.getDateFinPrevue()),
        "Chantier " + c.getCode() + " (" + c.getStatut() + ")");
  }

  @Override
  public void materiel(String type, Materiel m, Chantier chantierActuel, String motif) {
    outbox.publier(
        Topics.SUIVI,
        type,
        MATERIEL,
        m.getId(),
        new MaterielPayload(
            m.getId(),
            m.getCode(),
            m.getDesignation(),
            m.getCategorie().name(),
            m.getEtat().name(),
            m.getChantierActuelId(),
            chantierActuel == null ? null : chantierActuel.getNom(),
            m.getTarifJournalier(),
            m.getDescription().valeurAcquisition(),
            m.getHeuresFonctionnement(),
            m.getFournisseurId(),
            m.getContratId(),
            motif),
        "Matériel " + m.getCode() + " · " + m.getEtat() + (motif == null ? "" : " : " + motif));
  }

  @Override
  public void affectation(String type, Affectation a, Materiel m, Chantier c) {
    outbox.publier(
        Topics.SUIVI,
        type,
        AFFECTATION,
        a.getId(),
        new AffectationPayload(
            a.getId(),
            m.getId(),
            m.getCode(),
            m.getDesignation(),
            m.getCategorie().name(),
            m.getTarifJournalier(),
            c.getId(),
            c.getNom(),
            c.getChefChantierId(),
            a.getBesoinId(),
            a.getDateDebut(),
            a.getDateFinPrevue(),
            a.getDateFinReelle(),
            a.getStatut().name(),
            a.getFournisseurId(),
            a.getContratId()),
        "Affectation de " + m.getCode() + " à " + c.getCode() + " (" + a.getStatut() + ")");
  }

  @Override
  public void maintenance(String type, Maintenance mt, Materiel m) {
    outbox.publier(
        Topics.SUIVI,
        type,
        MAINTENANCE,
        mt.getId(),
        new MaintenancePayload(
            mt.getId(),
            mt.getNumeroOt(),
            m.getId(),
            m.getDesignation(),
            mt.getChantierId(),
            mt.getType().name(),
            mt.getStatut().name(),
            mt.getCout(),
            mt.getDatePlanifiee(),
            mt.getDateDebut(),
            mt.getDateFin()),
        "Ordre de travail "
            + mt.getNumeroOt()
            + " sur "
            + m.getCode()
            + " ("
            + mt.getStatut()
            + ")");
  }

  @Override
  public void panneSignalee(Panne p, Materiel m, Chantier c) {
    outbox.publier(
        Topics.SUIVI,
        EventTypes.PANNE_SIGNALEE,
        PANNE,
        p.getId(),
        new PannePayload(
            p.getId(),
            m.getId(),
            m.getDesignation(),
            p.getChantierId(),
            c == null ? null : c.getNom(),
            p.getGravite().name(),
            p.getDescription(),
            p.getSignaleParId(),
            p.getSignaleParNom()),
        "Panne " + p.getGravite() + " signalée sur " + m.getCode());
  }
}
