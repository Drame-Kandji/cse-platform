package sn.cse.achats.infrastructure.messaging;

import java.util.Optional;
import org.springframework.stereotype.Component;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.service.ParametresAchats;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.Attribution;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.FournisseurInvite;
import sn.cse.achats.domain.model.Offre;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.events.Topics;
import sn.cse.events.payload.AppelOffrePayload;
import sn.cse.events.payload.ContratPayload;
import sn.cse.events.payload.FacturePayload;

/**
 * Publie les événements de ms-achats via l'outbox transactionnelle (topic {@code
 * cse.achats.events}) ; l'entrée d'audit correspondante est produite par l'{@link OutboxWriter}.
 */
@Component
public class OutboxAchatsEventPublisher implements AchatsEventPublisher {

  static final String AGREGAT_AO = "AppelOffre";
  static final String AGREGAT_CONTRAT = "ContratAchat";
  static final String AGREGAT_FACTURE = "Facture";

  private final OutboxWriter outbox;
  private final ParametresAchats parametres;

  /**
   * @param outbox écrivain d'outbox
   * @param parametres taux du tarif journalier estimé
   */
  public OutboxAchatsEventPublisher(OutboxWriter outbox, ParametresAchats parametres) {
    this.outbox = outbox;
    this.parametres = parametres;
  }

  @Override
  public void appelOffre(String type, AppelOffre a) {
    // Proposition en cours ou marché attribué : l'offre concernée et son montant sont transmis.
    Optional<Attribution> attribution = a.getAttribution();
    Optional<Offre> offre = attribution.map(at -> a.offre(at.offreId()));
    AppelOffrePayload payload =
        new AppelOffrePayload(
            a.getId(),
            a.getReference(),
            a.getObjet(),
            a.getType().name(),
            a.getOrigine().map(o -> o.besoinId()).orElse(null),
            a.getOrigine().map(o -> o.chantierId()).orElse(null),
            a.getCategorie().name(),
            a.getQuantite(),
            a.getDateLimite(),
            a.getStatut().name(),
            a.getOffres().size(),
            a.getInvites().stream().map(FournisseurInvite::id).toList(),
            offre.map(Offre::getId).orElse(null),
            // Règle R10 : l'identité du soumissionnaire n'est jamais diffusée tant que PUBLIE.
            a.estAnonyme() ? null : offre.map(Offre::getFournisseurId).orElse(null),
            attribution.map(Attribution::montant).orElse(a.getMontantAttribue()),
            attribution.map(Attribution::approbationDgRequise).orElse(false));
    outbox.publier(
        Topics.ACHATS, type, AGREGAT_AO, a.getId(), payload, a.getReference() + " : " + type);
  }

  @Override
  public void contrat(String type, ContratAchat c) {
    ContratPayload payload =
        new ContratPayload(
            c.getId(),
            c.getNumero(),
            c.getAppelOffreId(),
            c.getBesoinId(),
            c.getChantierId(),
            c.getChantierNom(),
            c.getFournisseurId(),
            c.getFournisseurNom(),
            c.getDesignation(),
            c.getCategorie().name(),
            c.getQuantite(),
            c.getMontant(),
            c.getStatut().name(),
            c.getDateSignature(),
            c.getDateLivraisonPrevue(),
            c.getDateLivraisonReelle(),
            c.tarifJournalierEstime(parametres.contrat().tauxTarifJournalier()));
    outbox.publier(
        Topics.ACHATS, type, AGREGAT_CONTRAT, c.getId(), payload, c.getNumero() + " : " + type);
  }

  @Override
  public void facture(String type, Facture f) {
    FacturePayload payload =
        new FacturePayload(
            f.getId(),
            f.getNumero(),
            f.getContratId(),
            f.getFournisseurId(),
            f.getMontantTtc(),
            f.getStatut().name(),
            f.getDateEmission(),
            f.getDateEcheance(),
            f.getDateMandatement(),
            f.getDatePaiement());
    outbox.publier(
        Topics.ACHATS,
        type,
        AGREGAT_FACTURE,
        f.getId(),
        payload,
        "Facture " + f.getNumero() + " (" + f.getContratNumero() + ") : " + type);
  }
}
