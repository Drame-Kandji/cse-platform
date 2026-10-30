package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.port.in.GererCycleVieUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.DemandeBlacklistage;
import sn.cse.fournisseurs.domain.model.Fournisseur;

/** Transitions de statut des fournisseurs, dont le blacklistage en deux étapes (R09). */
@Service
@Transactional
public class CycleVieService implements GererCycleVieUseCase {

  private final FournisseurRepository fournisseurs;
  private final ChargeurFournisseur chargeur;
  private final FournisseurEventPublisher evenements;
  private final Clock clock;

  /**
   * @param fournisseurs dépôt
   * @param chargeur chargement et vues
   * @param evenements publication outbox
   * @param clock horloge
   */
  public CycleVieService(
      FournisseurRepository fournisseurs,
      ChargeurFournisseur chargeur,
      FournisseurEventPublisher evenements,
      Clock clock) {
    this.fournisseurs = fournisseurs;
    this.chargeur = chargeur;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public FournisseurVue validerInscription(UUID id, CurrentUser utilisateur) {
    return transition(id, Fournisseur::validerInscription, EventTypes.FOURNISSEUR_VALIDE, null);
  }

  @Override
  public FournisseurVue suspendre(UUID id, String motif, CurrentUser utilisateur) {
    return transition(id, f -> f.suspendre(motif), EventTypes.FOURNISSEUR_SUSPENDU, motif);
  }

  @Override
  public FournisseurVue reactiver(UUID id, CurrentUser utilisateur) {
    return transition(id, Fournisseur::reactiver, EventTypes.FOURNISSEUR_REACTIVE, null);
  }

  @Override
  public FournisseurVue demanderBlacklistage(UUID id, String motif, CurrentUser utilisateur) {
    // Règle R09 (étape 1) : le RA ne fait qu'une demande, le statut reste inchangé.
    return transition(
        id,
        f -> f.demanderBlacklistage(Auteurs.depuis(utilisateur), motif, clock.instant()),
        EventTypes.FOURNISSEUR_BLACKLISTAGE_DEMANDE,
        motif);
  }

  @Override
  public FournisseurVue deciderBlacklistage(
      UUID id, boolean approuve, String commentaire, CurrentUser utilisateur) {
    // Règle R09 (étape 2) : seule la décision de la DG fait passer le fournisseur en BLACKLISTE.
    Fournisseur fournisseur = chargeur.charger(id);
    DemandeBlacklistage demande =
        fournisseur.deciderBlacklistage(Auteurs.depuis(utilisateur), approuve, commentaire);
    Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
    if (approuve) {
      evenements.publierFournisseur(
          EventTypes.FOURNISSEUR_BLACKLISTE, persiste, persiste.getMotifStatut().orElse(null));
    } else {
      String motif =
          "Demande de "
              + demande.demandeurNom()
              + " refusée"
              + (commentaire == null || commentaire.isBlank() ? "" : " : " + commentaire.trim());
      evenements.publierFournisseur(
          FournisseurEventPublisher.FOURNISSEUR_BLACKLISTAGE_REFUSE, persiste, motif);
    }
    return chargeur.vue(persiste, false);
  }

  private FournisseurVue transition(
      UUID id, Consumer<Fournisseur> action, String typeEvenement, String motif) {
    Fournisseur fournisseur = chargeur.charger(id);
    action.accept(fournisseur);
    Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
    evenements.publierFournisseur(typeEvenement, persiste, motif == null ? null : motif.trim());
    return chargeur.vue(persiste, false);
  }
}
