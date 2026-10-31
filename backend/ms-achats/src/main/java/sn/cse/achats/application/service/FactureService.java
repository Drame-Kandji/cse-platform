package sn.cse.achats.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.application.port.in.GererFacturesUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.common.exception.ConflictException;
import sn.cse.events.EventTypes;

/** Réception, mandatement, paiement et rejet des factures fournisseurs. */
@Service
@Transactional
public class FactureService implements GererFacturesUseCase {

  private final FactureRepository factures;
  private final ContratRepository contrats;
  private final AchatsEventPublisher evenements;
  private final Clock clock;

  /**
   * @param factures dépôt des factures
   * @param contrats dépôt des contrats
   * @param evenements publication d'événements
   * @param clock horloge
   */
  public FactureService(
      FactureRepository factures,
      ContratRepository contrats,
      AchatsEventPublisher evenements,
      Clock clock) {
    this.factures = factures;
    this.contrats = contrats;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public Facture recevoir(RecevoirFactureCommand commande) {
    ContratAchat contrat = Chargements.contrat(contrats, commande.contratId());
    return recevoir(contrat, commande);
  }

  /**
   * @param contrat contrat déjà chargé (et contrôlé par l'appelant)
   * @param commande facture
   * @return la facture RECUE
   */
  Facture recevoir(ContratAchat contrat, RecevoirFactureCommand commande) {
    String numero = commande.numero() == null ? null : commande.numero().trim();
    if (numero != null && factures.existe(contrat.getFournisseurId(), numero)) {
      throw new ConflictException(
          CodesErreur.FACTURE_EXISTANTE,
          "La facture " + numero + " de ce fournisseur est déjà enregistrée.");
    }
    BigDecimal dejaFacture =
        factures.parContrat(contrat.getId()).stream()
            .filter(f -> f.getStatut() != StatutFacture.REJETEE)
            .map(Facture::getMontantTtc)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Facture facture =
        factures.enregistrer(
            Facture.recevoir(
                contrat,
                numero,
                commande.montantHt(),
                commande.tva(),
                commande.dateEmission(),
                commande.dateEcheance(),
                dejaFacture));
    evenements.facture(EventTypes.FACTURE_RECUE, facture);
    return facture;
  }

  @Override
  public Facture mandater(UUID factureId, String referenceMandat) {
    Facture facture = Chargements.facture(factures, factureId);
    facture.mandater(referenceMandat, LocalDate.now(clock));
    return publier(EventTypes.FACTURE_MANDATEE, facture);
  }

  @Override
  public Facture payer(UUID factureId) {
    Facture facture = Chargements.facture(factures, factureId);
    facture.payer(LocalDate.now(clock));
    return publier(EventTypes.FACTURE_PAYEE, facture);
  }

  @Override
  public Facture rejeter(UUID factureId, String motif) {
    Facture facture = Chargements.facture(factures, factureId);
    facture.rejeter(motif);
    return publier(EventTypes.FACTURE_REJETEE, facture);
  }

  private Facture publier(String type, Facture facture) {
    Facture enregistree = factures.enregistrer(facture);
    evenements.facture(type, enregistree);
    return enregistree;
  }
}
