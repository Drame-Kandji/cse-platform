package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ConflictException;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.InscriptionCommand;
import sn.cse.fournisseurs.application.port.in.InscrireFournisseurUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.exception.CodesErreur;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/**
 * Auto-inscription publique : le fournisseur est créé {@code EN_ATTENTE_VALIDATION} ; le compte du
 * portail ne sera créé qu'à la validation par le Responsable des Achats ({@code
 * FOURNISSEUR_VALIDE}, consommé par ms-auth).
 */
@Service
@Transactional
public class InscriptionService implements InscrireFournisseurUseCase {

  private final FournisseurRepository fournisseurs;
  private final ScorePolicy politique;
  private final FournisseurEventPublisher evenements;
  private final Clock clock;

  /**
   * @param fournisseurs dépôt
   * @param politique politique de score (score initial)
   * @param evenements publication outbox
   * @param clock horloge
   */
  public InscriptionService(
      FournisseurRepository fournisseurs,
      ScorePolicy politique,
      FournisseurEventPublisher evenements,
      Clock clock) {
    this.fournisseurs = fournisseurs;
    this.politique = politique;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public Fournisseur inscrire(InscriptionCommand commande) {
    ProfilFournisseur profil = commande.profil();
    // Message volontairement identique pour le NINEA et l'e-mail : l'endpoint est public, il ne
    // doit pas permettre de deviner quelle donnée est déjà enregistrée.
    if (fournisseurs.nineaUtilise(profil.ninea(), null)
        || fournisseurs.emailUtilise(profil.email())) {
      throw new ConflictException(
          CodesErreur.FOURNISSEUR_EXISTANT,
          "Un fournisseur est déjà enregistré avec ce NINEA ou cette adresse e-mail. Contactez le"
              + " service des achats de la CSE.");
    }
    Fournisseur fournisseur =
        Fournisseur.inscrire(profil, null, politique.scoreInitial(), LocalDate.now(clock));
    Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
    evenements.publierFournisseur(EventTypes.FOURNISSEUR_INSCRIT, persiste, null);
    return persiste;
  }
}
