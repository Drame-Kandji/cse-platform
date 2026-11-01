package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.ConfirmerLivraisonCommand;
import sn.cse.achats.application.command.ModifierClausesCommand;
import sn.cse.achats.application.port.in.ExecuterContratUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;

/**
 * Exécution des contrats côté CSE : clauses, signature du CSE, livraison (R06, démarrage de secours
 * hors portail), jalons, clôture.
 */
@Service
@Transactional
public class ExecutionContratService implements ExecuterContratUseCase {

  private final ContratRepository contrats;
  private final FactureRepository factures;
  private final AchatsEventPublisher evenements;
  private final ParametresAchats parametres;
  private final Clock clock;

  /**
   * @param contrats dépôt des contrats
   * @param factures dépôt des factures
   * @param evenements publication d'événements
   * @param parametres paramètres (plafond des pénalités)
   * @param clock horloge
   */
  public ExecutionContratService(
      ContratRepository contrats,
      FactureRepository factures,
      AchatsEventPublisher evenements,
      ParametresAchats parametres,
      Clock clock) {
    this.contrats = contrats;
    this.factures = factures;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public ContratAchat modifierClauses(ModifierClausesCommand commande) {
    ContratAchat contrat = Chargements.contrat(contrats, commande.contratId());
    contrat.modifierClauses(commande.clauses(), commande.delaiLivraisonJours(), commande.jalons());
    return contrats.enregistrer(contrat);
  }

  @Override
  public ContratAchat signer(UUID contratId, CurrentUser signataire) {
    ContratAchat contrat = Chargements.contrat(contrats, contratId);
    contrat.signerPourLeCse(Acteurs.de(signataire), Instant.now(clock));
    ContratAchat enregistre = contrats.enregistrer(contrat);
    // CONTRAT_SIGNE et ENGAGEMENT_DEMANDE suivront la signature du fournisseur sur le portail.
    evenements.contrat(EventTypes.CONTRAT_ENVOYE_SIGNATURE, enregistre);
    return enregistre;
  }

  @Override
  public ContratAchat demarrerLivraison(UUID contratId) {
    ContratAchat contrat = Chargements.contrat(contrats, contratId);
    contrat.demarrerLivraison();
    ContratAchat enregistre = contrats.enregistrer(contrat);
    evenements.contrat(EventTypes.LIVRAISON_DEMARREE, enregistre);
    return enregistre;
  }

  @Override
  public ContratAchat enregistrerJalon(UUID contratId, UUID jalonId, LocalDate dateReelle) {
    ContratAchat contrat = Chargements.contrat(contrats, contratId);
    contrat.enregistrerJalon(jalonId, dateReelle, LocalDate.now(clock));
    return contrats.enregistrer(contrat);
  }

  @Override
  public ContratAchat confirmerLivraison(ConfirmerLivraisonCommand commande) {
    ContratAchat contrat = Chargements.contrat(contrats, commande.contratId());
    contrat.confirmerLivraison(
        commande.dateLivraisonReelle(),
        commande.numeroBonLivraison(),
        commande.observations(),
        LocalDate.now(clock),
        parametres.contrat().plafondPenalitesPct());
    ContratAchat enregistre = contrats.enregistrer(contrat);
    // ms-suivi crée les matériels livrés à partir de cet événement.
    evenements.contrat(EventTypes.LIVRAISON_EFFECTUEE, enregistre);
    return enregistre;
  }

  @Override
  public ContratAchat cloturer(UUID contratId) {
    ContratAchat contrat = Chargements.contrat(contrats, contratId);
    long ouvertes =
        factures.parContrat(contratId).stream().filter(f -> f.getStatut().estOuverte()).count();
    if (ouvertes > 0) {
      throw new ConflictException(
          CodesErreur.FACTURES_EN_COURS,
          ouvertes + " facture(s) restent à payer : clôture du contrat impossible.");
    }
    contrat.cloturer();
    ContratAchat enregistre = contrats.enregistrer(contrat);
    evenements.contrat(EventTypes.CONTRAT_CLOTURE, enregistre);
    return enregistre;
  }
}
