package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.suivi.application.command.SignalerPanneCommande;
import sn.cse.suivi.application.port.in.SignalerPanneUseCase;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.PanneVue;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Panne;

/** Signalement d'une panne (chef de chantier : matériels de ses chantiers). */
@Service
@Transactional
public class SignalerPanneService implements SignalerPanneUseCase {

  private final PanneRepository pannes;
  private final ChargeurAgregats chargeur;
  private final SuiviEventPublisher evenements;
  private final Clock clock;

  /**
   * @param pannes stockage des pannes
   * @param chargeur chargement des agrégats
   * @param evenements publication des événements
   * @param clock horloge
   */
  public SignalerPanneService(
      PanneRepository pannes,
      ChargeurAgregats chargeur,
      SuiviEventPublisher evenements,
      Clock clock) {
    this.pannes = pannes;
    this.chargeur = chargeur;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public PanneVue signaler(SignalerPanneCommande commande) {
    Materiel materiel = chargeur.materiel(commande.materielId());
    if (commande.auteur().estRestreintAuxChantiers()
        && (materiel.getChantierActuelId() == null
            || !commande.auteur().peutAccederAuChantier(materiel.getChantierActuelId()))) {
      throw SuiviErreurs.chantierNonAutorise(materiel.getChantierActuelId());
    }
    Panne panne =
        pannes.save(
            Panne.signaler(
                materiel,
                commande.gravite(),
                commande.description(),
                commande.auteur().id(),
                commande.auteur().nom(),
                Instant.now(clock)));
    Chantier chantier = chargeur.chantierOuNull(panne.getChantierId());
    evenements.panneSignalee(panne, materiel, chantier);
    return new PanneVue(
        panne,
        materiel.getCode(),
        materiel.getDesignation(),
        chantier == null ? null : chantier.getNom());
  }
}
