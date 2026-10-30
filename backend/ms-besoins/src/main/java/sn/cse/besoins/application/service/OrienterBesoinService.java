package sn.cse.besoins.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import sn.cse.besoins.application.command.OrienterBesoinCommand;
import sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException;
import sn.cse.besoins.application.port.in.OrienterBesoinUseCase;
import sn.cse.besoins.application.port.out.AffectationMaterielPort;
import sn.cse.besoins.application.port.out.AffectationMaterielPort.DemandeAffectation;
import sn.cse.besoins.application.port.out.AffectationMaterielPort.ResultatAffectation;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort.DemandeDisponibilite;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort.Disponibilite;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.service.SelectionMateriels;

/**
 * Orientation d'un besoin validé : si ms-suivi annonce assez de matériels disponibles sur la
 * période, ils sont affectés (JWT de l'utilisateur relayé) et le besoin passe {@code
 * AFFECTE_DIRECT} ; sinon il passe {@code EN_ACHAT}. Si ms-suivi est indisponible, ou si des
 * conflits d'affectation empêchent de compléter la quantité, le besoin reste {@code VALIDE} avec
 * {@code orientationEnAttente=true} et les matériels déjà affectés sont conservés pour la relance.
 *
 * <p>Les appels distants sont faits hors transaction ; seul l'enregistrement du résultat est
 * transactionnel.
 */
@Service
public class OrienterBesoinService implements OrienterBesoinUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(OrienterBesoinService.class);

  private final EnregistrementBesoin enregistrement;
  private final DisponibiliteMaterielPort disponibilites;
  private final AffectationMaterielPort affectations;
  private final TransactionOperations transactions;
  private final Clock clock;

  OrienterBesoinService(
      EnregistrementBesoin enregistrement,
      DisponibiliteMaterielPort disponibilites,
      AffectationMaterielPort affectations,
      TransactionOperations transactions,
      Clock clock) {
    this.enregistrement = enregistrement;
    this.disponibilites = disponibilites;
    this.affectations = affectations;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Override
  public BesoinVue orienter(OrienterBesoinCommand commande) {
    Acteur acteur = Acteur.depuis(commande.utilisateur());
    BesoinMateriel besoin =
        transactions.execute(statut -> enregistrement.charger(commande.besoinId()));
    besoin.exigerOrientationEnAttente();
    Issue issue = tenter(besoin, commande.materielIds());
    BesoinMateriel oriente =
        transactions.execute(statut -> appliquer(commande.besoinId(), issue, acteur));
    return BesoinVue.depuis(oriente);
  }

  private Issue tenter(BesoinMateriel besoin, List<UUID> souhaites) {
    DescriptionBesoin d = besoin.description();
    int restant = besoin.quantiteRestanteAAffecter();
    Disponibilite disponibilite;
    try {
      disponibilite =
          disponibilites.verifier(
              new DemandeDisponibilite(
                  d.categorie(), d.designation(), restant, d.dateBesoin(), d.dateFinPrevue()));
    } catch (ServiceSuiviIndisponibleException e) {
      LOG.warn("Orientation du besoin {} reportée : ms-suivi indisponible", besoin.reference());
      return new Issue.Suspendue(
          List.of(), "Service de suivi du parc indisponible : orientation à relancer.");
    }
    List<UUID> candidats =
        SelectionMateriels.candidats(
            disponibilite.materielIds(), souhaites, besoin.materielIdsAffectes());
    if (candidats.size() < restant) {
      return new Issue.Achat(
          candidats.size()
              + " matériel(s) disponible(s) pour "
              + restant
              + " demandé(s) : transmission au service Achats.");
    }
    List<UUID> affectes = new ArrayList<>();
    for (UUID materiel : candidats) {
      if (affectes.size() == restant) {
        break;
      }
      try {
        ResultatAffectation resultat =
            affectations.affecter(
                new DemandeAffectation(
                    materiel, besoin.chantierId(), besoin.id(), d.dateBesoin(), d.dateFinPrevue()));
        if (resultat instanceof ResultatAffectation.Affecte) {
          affectes.add(materiel);
        } else if (resultat instanceof ResultatAffectation.Refuse refus) {
          LOG.info(
              "Affectation du matériel {} refusée pour {} : {}",
              materiel,
              besoin.reference(),
              refus.raison());
        }
      } catch (ServiceSuiviIndisponibleException e) {
        return new Issue.Suspendue(
            affectes,
            "Service de suivi du parc indisponible pendant l'affectation ("
                + affectes.size()
                + " matériel(s) affecté(s)) : orientation à relancer.");
      }
    }
    if (affectes.size() < restant) {
      return new Issue.Suspendue(
          affectes,
          "Conflits d'affectation : "
              + affectes.size()
              + " matériel(s) affecté(s) sur "
              + restant
              + ". Orientation à relancer.");
    }
    return new Issue.Complete(affectes);
  }

  private BesoinMateriel appliquer(UUID besoinId, Issue issue, Acteur acteur) {
    BesoinMateriel besoin = enregistrement.charger(besoinId);
    Instant maintenant = Instant.now(clock);
    if (issue instanceof Issue.Complete c) {
      besoin.affecterDirectement(c.affectes(), acteur, maintenant);
    } else if (issue instanceof Issue.Achat a) {
      besoin.orienterVersAchat(List.of(), acteur, a.commentaire(), maintenant);
    } else if (issue instanceof Issue.Suspendue s) {
      besoin.suspendreOrientation(s.affectes(), acteur, s.raison(), maintenant);
    }
    return enregistrement.enregistrerEtPublier(besoin);
  }

  /** Résultat de la tentative d'orientation, appliqué ensuite à l'agrégat. */
  private sealed interface Issue {

    record Complete(List<UUID> affectes) implements Issue {}

    record Achat(String commentaire) implements Issue {}

    record Suspendue(List<UUID> affectes, String raison) implements Issue {}
  }
}
