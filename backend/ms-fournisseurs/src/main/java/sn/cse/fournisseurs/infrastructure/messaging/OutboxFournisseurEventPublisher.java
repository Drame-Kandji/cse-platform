package sn.cse.fournisseurs.infrastructure.messaging;

import java.util.List;
import org.springframework.stereotype.Component;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.events.Topics;
import sn.cse.events.payload.FournisseurPayload;
import sn.cse.events.payload.LitigePayload;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.Litige;

/**
 * Publication des événements via l'outbox transactionnelle (topic {@code cse.fournisseurs.events} ;
 * l'entrée d'audit est générée par {@link OutboxWriter}).
 */
@Component
public class OutboxFournisseurEventPublisher implements FournisseurEventPublisher {

  static final String AGREGAT_FOURNISSEUR = "Fournisseur";
  static final String AGREGAT_LITIGE = "Litige";

  private final OutboxWriter outbox;

  /**
   * @param outbox écrivain outbox (cse-common)
   */
  public OutboxFournisseurEventPublisher(OutboxWriter outbox) {
    this.outbox = outbox;
  }

  @Override
  public void publierFournisseur(String type, Fournisseur fournisseur, String motif) {
    List<String> specialites =
        fournisseur.getProfil().specialites().stream().map(Enum::name).sorted().toList();
    FournisseurPayload payload =
        new FournisseurPayload(
            fournisseur.getId(),
            fournisseur.getProfil().raisonSociale(),
            fournisseur.getProfil().email(),
            specialites,
            fournisseur.getStatut().name(),
            fournisseur.getPerformance().scorePerformance(),
            motif);
    String resume =
        fournisseur.getProfil().raisonSociale()
            + " ("
            + fournisseur.getStatut()
            + ")"
            + (motif == null ? "" : " · " + motif);
    outbox.publier(
        Topics.FOURNISSEURS, type, AGREGAT_FOURNISSEUR, fournisseur.getId(), payload, resume);
  }

  @Override
  public void publierLitige(String type, Litige litige) {
    LitigePayload payload =
        new LitigePayload(
            litige.getId(),
            litige.getReference(),
            litige.getFournisseurId(),
            litige.getRaisonSociale(),
            litige.getContratId().orElse(null),
            litige.getObjet(),
            litige.getGravite().name(),
            litige.getStatut().name());
    String resume =
        litige.getReference() + " · " + litige.getRaisonSociale() + " : " + litige.getObjet();
    outbox.publier(Topics.FOURNISSEURS, type, AGREGAT_LITIGE, litige.getId(), payload, resume);
  }
}
