package sn.cse.reporting.application.service;

import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.FournisseurPayload;
import sn.cse.events.payload.LitigePayload;
import sn.cse.reporting.application.command.EvenementRecu;
import sn.cse.reporting.application.port.in.ProjeterFournisseursUseCase;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.GraviteLitige;
import sn.cse.reporting.domain.model.StatutFournisseur;
import sn.cse.reporting.domain.model.StatutLitige;
import sn.cse.reporting.domain.model.ValeursEnum;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;

/** Projection des événements de ms-fournisseurs : statut, score (R07) et litiges. */
@Service
@Transactional
public class ProjectionFournisseursService implements ProjeterFournisseursUseCase {

  /** Types ajoutés par ms-fournisseurs, pas encore déclarés dans {@code EventTypes}. */
  static final String FOURNISSEUR_MODIFIE = "FOURNISSEUR_MODIFIE";

  /** Refus du blacklistage par la DG (R09), même payload. */
  static final String FOURNISSEUR_BLACKLISTAGE_REFUSE = "FOURNISSEUR_BLACKLISTAGE_REFUSE";

  private static final Set<String> FOURNISSEUR =
      Set.of(
          FOURNISSEUR_MODIFIE,
          FOURNISSEUR_BLACKLISTAGE_REFUSE,
          EventTypes.FOURNISSEUR_INSCRIT,
          EventTypes.FOURNISSEUR_VALIDE,
          EventTypes.FOURNISSEUR_SUSPENDU,
          EventTypes.FOURNISSEUR_REACTIVE,
          EventTypes.FOURNISSEUR_BLACKLISTAGE_DEMANDE,
          EventTypes.FOURNISSEUR_BLACKLISTE,
          EventTypes.SCORE_RECALCULE,
          EventTypes.DOCUMENT_EXPIRE);

  // LITIGE_RESOLU est aussi émis pour un litige classé (statut CLASSE) : le statut du payload fait
  // foi.
  private static final Set<String> LITIGE =
      Set.of(EventTypes.LITIGE_OUVERT, EventTypes.LITIGE_RESOLU);

  private final ProjectionRepository vues;

  /**
   * @param vues persistance des vues
   */
  public ProjectionFournisseursService(ProjectionRepository vues) {
    this.vues = vues;
  }

  @Override
  public boolean projeter(EvenementRecu e) {
    if (FOURNISSEUR.contains(e.type())) {
      fournisseur(e.payload(FournisseurPayload.class), e.occurredAt());
      return true;
    }
    if (LITIGE.contains(e.type())) {
      litige(e.payload(LitigePayload.class), e.occurredAt());
      return true;
    }
    return false;
  }

  private void fournisseur(FournisseurPayload p, Instant quand) {
    FournisseurVue recu =
        new FournisseurVue(
            p.fournisseurId(),
            p.raisonSociale(),
            ValeursEnum.lire(StatutFournisseur.class, p.statut()),
            p.scorePerformance(),
            true,
            quand);
    vues.enregistrer(
        vues.fournisseur(p.fournisseurId()).map(f -> f.fusionnerAvec(recu)).orElse(recu));
  }

  private void litige(LitigePayload p, Instant quand) {
    LitigeVue recu =
        new LitigeVue(
            p.litigeId(),
            p.reference(),
            p.fournisseurId(),
            p.contratId(),
            ValeursEnum.lire(GraviteLitige.class, p.gravite()),
            ValeursEnum.lire(StatutLitige.class, p.statut()),
            quand,
            quand);
    vues.enregistrer(vues.litige(p.litigeId()).map(l -> l.fusionnerAvec(recu)).orElse(recu));
    if (p.fournisseurId() != null) {
      vues.referencer(FournisseurVue.reference(p.fournisseurId(), p.raisonSociale(), quand));
    }
  }
}
