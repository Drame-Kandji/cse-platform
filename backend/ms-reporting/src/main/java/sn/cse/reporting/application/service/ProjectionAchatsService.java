package sn.cse.reporting.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AppelOffrePayload;
import sn.cse.events.payload.ContratPayload;
import sn.cse.events.payload.FacturePayload;
import sn.cse.reporting.application.command.EvenementRecu;
import sn.cse.reporting.application.port.in.ProjeterAchatsUseCase;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.StatutAppelOffre;
import sn.cse.reporting.domain.model.StatutContrat;
import sn.cse.reporting.domain.model.StatutFacture;
import sn.cse.reporting.domain.model.ValeursEnum;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;

/** Projection des événements de ms-achats : appels d'offres, contrats, engagements, factures. */
@Service
@Transactional
public class ProjectionAchatsService implements ProjeterAchatsUseCase {

  private static final Set<String> APPEL_OFFRE =
      Set.of(
          EventTypes.AO_PUBLIE,
          EventTypes.OFFRE_DEPOSEE,
          EventTypes.AO_CLOTURE,
          EventTypes.ATTRIBUTION_PROPOSEE,
          EventTypes.ATTRIBUTION_REFUSEE,
          EventTypes.MARCHE_ATTRIBUE);

  private static final Set<String> CONTRAT =
      Set.of(
          EventTypes.CONTRAT_GENERE,
          EventTypes.CONTRAT_ENVOYE_SIGNATURE,
          EventTypes.CONTRAT_SIGNATURE_REFUSEE,
          EventTypes.CONTRAT_SIGNE,
          EventTypes.ENGAGEMENT_DEMANDE,
          EventTypes.ENGAGEMENT_VALIDE,
          EventTypes.ENGAGEMENT_REJETE,
          EventTypes.LIVRAISON_DEMARREE,
          EventTypes.LIVRAISON_EFFECTUEE,
          EventTypes.ALERTE_RETARD_LIVRAISON,
          EventTypes.CONTRAT_CLOTURE);

  private static final Set<String> FACTURE =
      Set.of(
          EventTypes.FACTURE_RECUE,
          EventTypes.FACTURE_MANDATEE,
          EventTypes.FACTURE_PAYEE,
          EventTypes.FACTURE_REJETEE);

  private final ProjectionRepository vues;

  /**
   * @param vues persistance des vues
   */
  public ProjectionAchatsService(ProjectionRepository vues) {
    this.vues = vues;
  }

  @Override
  public boolean projeter(EvenementRecu e) {
    if (APPEL_OFFRE.contains(e.type())) {
      appelOffre(e.payload(AppelOffrePayload.class), e.occurredAt());
    } else if (CONTRAT.contains(e.type())) {
      contrat(e.type(), e.payload(ContratPayload.class), e.occurredAt());
    } else if (FACTURE.contains(e.type())) {
      facture(e.payload(FacturePayload.class), e.occurredAt());
    } else {
      return false;
    }
    return true;
  }

  private void appelOffre(AppelOffrePayload p, Instant quand) {
    AppelOffreVue recu =
        new AppelOffreVue(
            p.appelOffreId(),
            p.reference(),
            p.chantierId(),
            p.besoinId(),
            ValeursEnum.lire(CategorieMateriel.class, p.categorie()),
            ValeursEnum.lire(StatutAppelOffre.class, p.statut()),
            quand,
            p.dateLimite(),
            p.montantAttribue(),
            p.fournisseurRetenuId(),
            quand);
    vues.enregistrer(
        vues.appelOffre(p.appelOffreId()).map(a -> a.fusionnerAvec(recu)).orElse(recu));
  }

  private void contrat(String type, ContratPayload p, Instant quand) {
    StatutContrat statut = ValeursEnum.lire(StatutContrat.class, p.statut());
    // ENGAGEMENT_VALIDE fait foi même si le statut de l'instantané n'a pas encore évolué.
    LocalDate engagement =
        ContratVue.dateEngagementDeduite(
            EventTypes.ENGAGEMENT_VALIDE.equals(type) ? StatutContrat.ENGAGEMENT_VALIDE : statut,
            p.dateSignature(),
            quand);
    ContratVue recu =
        new ContratVue(
            p.contratId(),
            p.numero(),
            p.appelOffreId(),
            p.chantierId(),
            p.fournisseurId(),
            p.fournisseurNom(),
            p.montant(),
            statut,
            p.dateSignature(),
            engagement,
            p.dateLivraisonPrevue(),
            p.dateLivraisonReelle(),
            quand);
    vues.enregistrer(vues.contrat(p.contratId()).map(c -> c.fusionnerAvec(recu)).orElse(recu));
    if (p.chantierId() != null) {
      vues.referencer(ChantierVue.reference(p.chantierId(), p.chantierNom(), quand));
    }
    if (p.fournisseurId() != null) {
      vues.referencer(FournisseurVue.reference(p.fournisseurId(), p.fournisseurNom(), quand));
    }
  }

  private void facture(FacturePayload p, Instant quand) {
    StatutFacture statut = ValeursEnum.lire(StatutFacture.class, p.statut());
    // Dates métier du payload ; repli sur le jour de l'événement pour les anciens événements.
    LocalDate[] dates =
        FactureVue.datesDeduites(
            statut,
            new FactureVue.DatesMetier(p.dateEmission(), p.dateMandatement(), p.datePaiement()),
            quand);
    FactureVue recu =
        new FactureVue(
            p.factureId(),
            p.numero(),
            p.contratId(),
            p.fournisseurId(),
            p.montantTtc(),
            statut,
            p.dateEcheance(),
            dates[0],
            dates[1],
            dates[2],
            quand);
    vues.enregistrer(vues.facture(p.factureId()).map(f -> f.fusionnerAvec(recu)).orElse(recu));
  }
}
