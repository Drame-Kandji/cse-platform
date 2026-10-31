package sn.cse.achats.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.DeposerOffreCommand;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.application.port.in.DeposerOffreUseCase;
import sn.cse.achats.application.port.in.OffreFournisseur;
import sn.cse.achats.application.port.in.PortailFournisseurUseCase;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/**
 * Portail fournisseur : chaque lecture et écriture est restreinte au fournisseur rattaché au jeton
 * (claim {@code fournisseur_id}).
 */
@Service
@Transactional
public class PortailFournisseurService implements PortailFournisseurUseCase {

  private final AppelOffreRepository appelsOffres;
  private final ContratRepository contrats;
  private final FactureRepository factures;
  private final DeposerOffreUseCase depot;
  private final FactureService facturation;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param contrats dépôt des contrats
   * @param factures dépôt des factures
   * @param depot dépôt des offres
   * @param facturation traitement des factures
   * @param clock horloge
   */
  public PortailFournisseurService(
      AppelOffreRepository appelsOffres,
      ContratRepository contrats,
      FactureRepository factures,
      DeposerOffreUseCase depot,
      FactureService facturation,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.contrats = contrats;
    this.factures = factures;
    this.depot = depot;
    this.facturation = facturation;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AppelOffre> appelsOffresOuverts(CurrentUser fournisseur, PageQuery page) {
    return appelsOffres.ouvertsAuFournisseur(fournisseurDe(fournisseur), Instant.now(clock), page);
  }

  @Override
  public OffreFournisseur deposerOffre(
      CurrentUser fournisseur,
      UUID appelOffreId,
      BigDecimal montant,
      int delaiLivraisonJours,
      Integer garantieMois,
      String description) {
    UUID id = fournisseurDe(fournisseur);
    AppelOffre appelOffre =
        appelsOffres
            .parId(appelOffreId)
            .filter(ao -> ao.estOuvertAuFournisseur(id))
            // Un appel d'offres restreint non ouvert à ce fournisseur est traité comme inexistant.
            .orElseThrow(() -> new ResourceNotFoundException("AppelOffre", appelOffreId));
    return depot.deposerDepuisPortail(
        new DeposerOffreCommand(
            appelOffre.getId(),
            id,
            montant,
            delaiLivraisonJours,
            garantieMois,
            description,
            fournisseur));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<OffreFournisseur> mesOffres(CurrentUser fournisseur, PageQuery page) {
    UUID id = fournisseurDe(fournisseur);
    return appelsOffres
        .avecOffreDu(id, page)
        .map(ao -> new OffreFournisseur(ao, ao.offreDu(id).orElseThrow()));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ContratAchat> attributions(CurrentUser fournisseur, PageQuery page) {
    return contrats.rechercher(
        new ContratRepository.Filtre(null, fournisseurDe(fournisseur), null), page);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<Facture> factures(CurrentUser fournisseur, PageQuery page) {
    return factures.rechercher(
        new FactureRepository.Filtre(null, fournisseurDe(fournisseur), null, null),
        LocalDate.now(clock),
        page);
  }

  @Override
  public Facture deposerFacture(RecevoirFactureCommand commande) {
    UUID id = fournisseurDe(commande.auteur());
    ContratAchat contrat =
        contrats
            .parId(commande.contratId())
            .filter(c -> c.getFournisseurId().equals(id))
            .orElseThrow(() -> new ResourceNotFoundException("Contrat", commande.contratId()));
    return facturation.recevoir(contrat, commande);
  }

  private static UUID fournisseurDe(CurrentUser utilisateur) {
    return utilisateur
        .fournisseur()
        .orElseThrow(
            () ->
                new ForbiddenOperationException(
                    CodesErreur.COMPTE_NON_RATTACHE,
                    "Ce compte n'est rattaché à aucun fournisseur."));
  }
}
