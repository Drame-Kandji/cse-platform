package sn.cse.besoins.infrastructure.messaging;

import org.springframework.stereotype.Component;
import sn.cse.besoins.application.port.out.BesoinEventPublisher;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.model.EvenementBesoin;
import sn.cse.besoins.domain.model.TypeEvenementBesoin;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.BesoinPayload;

/**
 * Publie les événements BESOIN_* sur {@code cse.besoins.events} via l'outbox transactionnelle
 * (l'entrée d'audit est ajoutée par {@link OutboxWriter}).
 */
@Component
public class OutboxBesoinEventPublisher implements BesoinEventPublisher {

  /** Type d'agrégat porté par l'enveloppe. */
  public static final String AGREGAT = "BesoinMateriel";

  private final OutboxWriter outbox;

  /**
   * @param outbox écrivain d'outbox (transaction courante obligatoire)
   */
  public OutboxBesoinEventPublisher(OutboxWriter outbox) {
    this.outbox = outbox;
  }

  @Override
  public void publier(BesoinMateriel besoin, EvenementBesoin evenement) {
    outbox.publier(
        Topics.BESOINS,
        type(evenement.type()),
        AGREGAT,
        besoin.id(),
        payload(besoin),
        resume(besoin, evenement.type()));
  }

  /**
   * @param besoin besoin
   * @return instantané publié
   */
  static BesoinPayload payload(BesoinMateriel besoin) {
    DescriptionBesoin d = besoin.description();
    return new BesoinPayload(
        besoin.id(),
        besoin.reference(),
        besoin.chantierId(),
        besoin.chantierNom(),
        besoin.demandeurId(),
        besoin.demandeurNom(),
        d.designation(),
        d.categorie().name(),
        d.quantite(),
        d.specifications(),
        d.priorite().name(),
        besoin.statut().name(),
        d.dateBesoin(),
        d.dureeUtilisationJours(),
        besoin.motifRejet(),
        besoin.materielIdsAffectes());
  }

  static String type(TypeEvenementBesoin type) {
    return switch (type) {
      case SOUMIS -> EventTypes.BESOIN_SOUMIS;
      case MODIFIE -> EventTypes.BESOIN_MODIFIE;
      case VALIDE -> EventTypes.BESOIN_VALIDE;
      case REJETE -> EventTypes.BESOIN_REJETE;
      case AFFECTE_DIRECT -> EventTypes.BESOIN_AFFECTE_DIRECT;
      case EN_ACHAT -> EventTypes.BESOIN_EN_ACHAT;
    };
  }

  private static String resume(BesoinMateriel besoin, TypeEvenementBesoin type) {
    String action =
        switch (type) {
          case SOUMIS -> "soumis";
          case MODIFIE -> "modifié";
          case VALIDE -> "validé";
          case REJETE -> "rejeté";
          case AFFECTE_DIRECT -> "satisfait par affectation directe";
          case EN_ACHAT -> "transmis aux achats";
        };
    return "Besoin "
        + besoin.reference()
        + " ("
        + besoin.description().quantite()
        + " × "
        + besoin.description().designation()
        + ", "
        + besoin.chantierNom()
        + ") "
        + action;
  }
}
