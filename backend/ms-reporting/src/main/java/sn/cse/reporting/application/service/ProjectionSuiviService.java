package sn.cse.reporting.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.events.payload.ChantierPayload;
import sn.cse.events.payload.MaintenancePayload;
import sn.cse.events.payload.MaterielPayload;
import sn.cse.events.payload.PannePayload;
import sn.cse.reporting.application.command.EvenementRecu;
import sn.cse.reporting.application.port.in.PhotographierParcUseCase;
import sn.cse.reporting.application.port.in.ProjeterSuiviUseCase;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.GravitePanne;
import sn.cse.reporting.domain.model.StatutAffectation;
import sn.cse.reporting.domain.model.StatutChantier;
import sn.cse.reporting.domain.model.StatutMaintenance;
import sn.cse.reporting.domain.model.TypeMaintenance;
import sn.cse.reporting.domain.model.ValeursEnum;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;

/**
 * Projection des événements de ms-suivi : chantiers, matériels, affectations, maintenances, pannes.
 * Chaque changement d'état du parc rafraîchit l'instantané du jour.
 */
@Service
@Transactional
public class ProjectionSuiviService implements ProjeterSuiviUseCase {

  private final ProjectionRepository vues;
  private final PhotographierParcUseCase photographe;
  private final Clock clock;

  /**
   * @param vues persistance des vues
   * @param photographe instantané du parc
   * @param clock horloge
   */
  public ProjectionSuiviService(
      ProjectionRepository vues, PhotographierParcUseCase photographe, Clock clock) {
    this.vues = vues;
    this.photographe = photographe;
    this.clock = clock;
  }

  @Override
  public boolean projeter(EvenementRecu e) {
    switch (e.type()) {
      case EventTypes.CHANTIER_CREE, EventTypes.CHANTIER_MODIFIE ->
          chantier(e.payload(ChantierPayload.class), e.occurredAt());
      case EventTypes.MATERIEL_ENREGISTRE,
          EventTypes.MATERIEL_ETAT_CHANGE,
          EventTypes.MATERIEL_REFORME_DEMANDEE,
          EventTypes.MATERIEL_REFORME,
          EventTypes.ALERTE_MAINTENANCE -> {
        materiel(e.type(), e.payload(MaterielPayload.class), e.occurredAt());
        photographierAujourdhui();
      }
      case EventTypes.MATERIEL_AFFECTE,
          EventTypes.RECEPTION_CONFIRMEE,
          EventTypes.AFFECTATION_TERMINEE,
          EventTypes.FIN_AFFECTATION_PROCHAINE -> {
        affectation(e.type(), e.payload(AffectationPayload.class), e.occurredAt());
        photographierAujourdhui();
      }
      case EventTypes.MAINTENANCE_PLANIFIEE,
          EventTypes.MAINTENANCE_DEMARREE,
          EventTypes.MAINTENANCE_CLOTUREE -> {
        maintenance(e.type(), e.payload(MaintenancePayload.class), e.occurredAt());
        photographierAujourdhui();
      }
      case EventTypes.PANNE_SIGNALEE -> panne(e.payload(PannePayload.class), e.occurredAt());
      default -> {
        return false;
      }
    }
    return true;
  }

  private void chantier(ChantierPayload p, Instant quand) {
    ChantierVue recu =
        new ChantierVue(
            p.chantierId(),
            p.code(),
            p.nom(),
            p.region(),
            ValeursEnum.lire(StatutChantier.class, p.statut()),
            p.budget(),
            p.montantMarche(),
            p.chefChantierId(),
            p.dateDebut(),
            p.dateFinPrevue(),
            true,
            quand);
    vues.enregistrer(vues.chantier(p.chantierId()).map(c -> c.fusionnerAvec(recu)).orElse(recu));
  }

  private void materiel(String type, MaterielPayload p, Instant quand) {
    MaterielVue recu =
        new MaterielVue(
            p.materielId(),
            p.code(),
            p.designation(),
            ValeursEnum.lire(CategorieMateriel.class, p.categorie()),
            ValeursEnum.lire(EtatMateriel.class, p.etat()),
            p.chantierId(),
            p.tarifJournalier(),
            p.valeurAcquisition(),
            EventTypes.ALERTE_MAINTENANCE.equals(type) ? quand : null,
            quand);
    vues.enregistrer(vues.materiel(p.materielId()).map(m -> m.fusionnerAvec(recu)).orElse(recu));
    if (p.chantierId() != null) {
      referencerChantier(p.chantierId(), p.chantierNom(), quand);
    }
  }

  private void affectation(String type, AffectationPayload p, Instant quand) {
    CategorieMateriel categorie = ValeursEnum.lire(CategorieMateriel.class, p.categorie());
    StatutAffectation statut = ValeursEnum.lire(StatutAffectation.class, p.statut());
    AffectationVue recu =
        new AffectationVue(
            p.affectationId(),
            p.materielId(),
            categorie,
            p.chantierId(),
            p.tarifJournalier(),
            p.besoinId(),
            p.dateDebut(),
            p.dateFinPrevue(),
            p.dateFinReelle(),
            statut,
            quand);
    vues.enregistrer(
        vues.affectation(p.affectationId()).map(a -> a.fusionnerAvec(recu)).orElse(recu));
    referencerChantier(p.chantierId(), p.chantierNom(), quand);

    MaterielVue materiel =
        vues.materiel(p.materielId())
            .orElseGet(
                () ->
                    MaterielVue.reference(
                        p.materielId(),
                        p.materielCode(),
                        p.materielDesignation(),
                        categorie,
                        p.tarifJournalier()));
    // L'état du matériel est déduit défensivement : l'événement MATERIEL_ETAT_CHANGE éventuel,
    // plus récent, l'emporte grâce à l'horodatage.
    MaterielVue maj =
        switch (type) {
          case EventTypes.AFFECTATION_TERMINEE ->
              materiel.avecEtat(EtatMateriel.DISPONIBLE, null, quand);
          case EventTypes.MATERIEL_AFFECTE, EventTypes.RECEPTION_CONFIRMEE ->
              statut == StatutAffectation.TERMINEE
                  ? materiel
                  : materiel.avecEtat(EtatMateriel.AFFECTE, p.chantierId(), quand);
          default -> materiel;
        };
    vues.enregistrer(maj);
  }

  private void maintenance(String type, MaintenancePayload p, Instant quand) {
    boolean cloture = EventTypes.MAINTENANCE_CLOTUREE.equals(type);
    MaintenanceVue recu =
        new MaintenanceVue(
            p.maintenanceId(),
            p.numeroOt(),
            p.materielId(),
            p.chantierId(),
            ValeursEnum.lire(TypeMaintenance.class, p.type()),
            ValeursEnum.lire(StatutMaintenance.class, p.statut()),
            p.cout(),
            p.datePlanifiee(),
            p.dateDebut(),
            p.dateFin(),
            cloture ? quand : null,
            quand);
    vues.enregistrer(
        vues.maintenance(p.maintenanceId()).map(m -> m.fusionnerAvec(recu)).orElse(recu));
    if (EventTypes.MAINTENANCE_PLANIFIEE.equals(type)) {
      return;
    }
    MaterielVue materiel =
        vues.materiel(p.materielId())
            .orElseGet(
                () ->
                    MaterielVue.reference(
                        p.materielId(), null, p.materielDesignation(), null, null));
    MaterielVue maj =
        cloture
            ? materiel.avecEtat(
                materiel.chantierId() != null ? EtatMateriel.AFFECTE : EtatMateriel.DISPONIBLE,
                materiel.chantierId(),
                quand)
            : materiel.avecEtat(EtatMateriel.EN_MAINTENANCE, materiel.chantierId(), quand);
    vues.enregistrer(maj.alerteTraiteeLe(quand));
    if (cloture) {
      for (PanneVue panne : vues.pannesOuvertes(p.materielId())) {
        PanneVue resolue = panne.resolueSi(quand);
        if (resolue != panne) {
          vues.enregistrer(resolue);
        }
      }
    }
  }

  private void panne(PannePayload p, Instant quand) {
    if (vues.panne(p.panneId()).isPresent()) {
      return;
    }
    PanneVue panne =
        new PanneVue(
            p.panneId(),
            p.materielId(),
            p.chantierId(),
            ValeursEnum.lire(GravitePanne.class, p.gravite()),
            quand,
            null);
    // Une clôture de maintenance du même matériel, postérieure et déjà reçue, résout la panne.
    vues.enregistrer(
        vues.derniereClotureMaintenance(p.materielId()).map(panne::resolueSi).orElse(panne));
    if (p.chantierId() != null) {
      referencerChantier(p.chantierId(), p.chantierNom(), quand);
    }
  }

  private void referencerChantier(UUID chantierId, String nom, Instant quand) {
    if (chantierId == null) {
      return;
    }
    vues.referencer(ChantierVue.reference(chantierId, nom, quand));
  }

  private void photographierAujourdhui() {
    photographe.photographier(LocalDate.now(clock));
  }
}
