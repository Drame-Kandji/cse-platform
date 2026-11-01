package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.ContratsPortailUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;

/**
 * Exécution des contrats côté fournisseur (portail). Chaque opération charge le contrat filtré sur
 * le fournisseur du jeton : un contrat d'un autre fournisseur renvoie 404, jamais 403, pour ne pas
 * révéler son existence.
 */
@Service
@Transactional
public class ContratsPortailService implements ContratsPortailUseCase {

  private final ContratRepository contrats;
  private final AchatsEventPublisher evenements;
  private final Clock clock;

  /**
   * @param contrats dépôt des contrats
   * @param evenements publication d'événements
   * @param clock horloge
   */
  public ContratsPortailService(
      ContratRepository contrats, AchatsEventPublisher evenements, Clock clock) {
    this.contrats = contrats;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ContratAchat> contrats(
      CurrentUser fournisseur, StatutContrat statut, PageQuery page) {
    return contrats.rechercher(
        new ContratRepository.Filtre(statut, ComptesPortail.fournisseurDe(fournisseur), null, null),
        page);
  }

  @Override
  @Transactional(readOnly = true)
  public ContratAchat contrat(CurrentUser fournisseur, UUID contratId) {
    return charger(fournisseur, contratId);
  }

  @Override
  public ContratAchat signer(CurrentUser fournisseur, UUID contratId) {
    ContratAchat contrat = charger(fournisseur, contratId);
    contrat.signerPourLeFournisseur(Acteurs.de(fournisseur), Instant.now(clock));
    ContratAchat enregistre = contrats.enregistrer(contrat);
    // Même enchaînement qu'avant le portail : le contrat signé déclenche la demande d'engagement.
    evenements.contrat(EventTypes.CONTRAT_SIGNE, enregistre);
    evenements.contrat(EventTypes.ENGAGEMENT_DEMANDE, enregistre);
    return enregistre;
  }

  @Override
  public ContratAchat refuser(CurrentUser fournisseur, UUID contratId, String motif) {
    ContratAchat contrat = charger(fournisseur, contratId);
    contrat.refuserSignature(motif);
    ContratAchat enregistre = contrats.enregistrer(contrat);
    evenements.contrat(EventTypes.CONTRAT_SIGNATURE_REFUSEE, enregistre);
    return enregistre;
  }

  @Override
  public ContratAchat expedier(
      CurrentUser fournisseur, UUID contratId, LocalDate dateExpedition, String commentaire) {
    ContratAchat contrat = charger(fournisseur, contratId);
    contrat.declarerExpedition(dateExpedition, commentaire, LocalDate.now(clock));
    ContratAchat enregistre = contrats.enregistrer(contrat);
    evenements.contrat(EventTypes.LIVRAISON_DEMARREE, enregistre);
    return enregistre;
  }

  @Override
  public ContratAchat enregistrerJalon(
      CurrentUser fournisseur, UUID contratId, UUID jalonId, LocalDate dateReelle) {
    ContratAchat contrat = charger(fournisseur, contratId);
    contrat.enregistrerJalonDuFournisseur(jalonId, dateReelle, LocalDate.now(clock));
    return contrats.enregistrer(contrat);
  }

  private ContratAchat charger(CurrentUser fournisseur, UUID contratId) {
    UUID id = ComptesPortail.fournisseurDe(fournisseur);
    return contrats
        .parId(contratId)
        .filter(c -> c.getFournisseurId().equals(id))
        .orElseThrow(() -> new ResourceNotFoundException("Contrat", contratId));
  }
}
