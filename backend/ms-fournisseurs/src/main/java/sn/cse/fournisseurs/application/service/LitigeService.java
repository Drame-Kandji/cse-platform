package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.LitigeCommand;
import sn.cse.fournisseurs.application.port.in.GererLitigesUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.GenerateurReference;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.domain.model.Auteur;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.StatutLitige;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;

/** Ouverture et suivi des litiges ; la clôture d'un litige déclenche le recalcul du score (R07). */
@Service
@Transactional
public class LitigeService implements GererLitigesUseCase {

  private final LitigeRepository litiges;
  private final ChargeurFournisseur chargeur;
  private final GenerateurReference references;
  private final RecalculScore recalcul;
  private final FournisseurEventPublisher evenements;
  private final Clock clock;

  /**
   * @param litiges dépôt des litiges
   * @param chargeur chargement des fournisseurs
   * @param references générateur de références
   * @param recalcul recalcul du score
   * @param evenements publication outbox
   * @param clock horloge
   */
  public LitigeService(
      LitigeRepository litiges,
      ChargeurFournisseur chargeur,
      GenerateurReference references,
      RecalculScore recalcul,
      FournisseurEventPublisher evenements,
      Clock clock) {
    this.litiges = litiges;
    this.chargeur = chargeur;
    this.references = references;
    this.recalcul = recalcul;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public Litige ouvrir(UUID fournisseurId, LitigeCommand commande, CurrentUser utilisateur) {
    return ouvrir(
        chargeur.charger(fournisseurId), commande, Auteurs.depuis(utilisateur), clock.instant());
  }

  /**
   * Ouvre un litige à une date donnée (utilisé aussi par le jeu de démonstration).
   *
   * @param fournisseur fournisseur
   * @param commande données
   * @param auteur auteur
   * @param date instant d'ouverture
   * @return litige persisté
   */
  public Litige ouvrir(
      Fournisseur fournisseur, LitigeCommand commande, Auteur auteur, Instant date) {
    String reference =
        references.prochaineReferenceLitige(LocalDate.ofInstant(date, clock.getZone()).getYear());
    Litige litige =
        litiges.sauvegarder(
            Litige.ouvrir(
                reference,
                fournisseur,
                commande.contratId(),
                commande.objet(),
                commande.description(),
                commande.gravite(),
                auteur,
                date));
    evenements.publierLitige(EventTypes.LITIGE_OUVERT, litige);
    recalcul.recalculer(fournisseur, "Litige " + reference + " ouvert");
    return litige;
  }

  @Override
  public Litige agir(
      UUID litigeId, TypeActionLitige type, String commentaire, CurrentUser utilisateur) {
    return agir(litigeId, type, commentaire, Auteurs.depuis(utilisateur), clock.instant());
  }

  /**
   * Ajoute une action à une date donnée (utilisé aussi par le jeu de démonstration).
   *
   * @param litigeId litige
   * @param type type d'action
   * @param commentaire commentaire
   * @param auteur auteur
   * @param date instant
   * @return litige persisté
   */
  public Litige agir(
      UUID litigeId, TypeActionLitige type, String commentaire, Auteur auteur, Instant date) {
    Litige litige =
        litiges
            .parId(litigeId)
            .orElseThrow(() -> new ResourceNotFoundException("Litige", litigeId));
    boolean clos = litige.agir(type, commentaire, auteur, date);
    Litige persiste = litiges.sauvegarder(litige);
    if (clos) {
      // LITIGE_RESOLU couvre la résolution et le classement (le statut du payload les distingue).
      evenements.publierLitige(EventTypes.LITIGE_RESOLU, persiste);
      recalcul.recalculer(
          chargeur.charger(persiste.getFournisseurId()),
          "Litige "
              + persiste.getReference()
              + " "
              + persiste.getStatut().name().toLowerCase(java.util.Locale.ROOT));
    }
    return persiste;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<Litige> rechercher(StatutLitige statut, UUID fournisseurId, PageQuery page) {
    return litiges.rechercher(statut, fournisseurId, page);
  }
}
