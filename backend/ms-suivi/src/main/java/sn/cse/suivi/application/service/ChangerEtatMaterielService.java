package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.port.in.ChangerEtatMaterielUseCase;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Changement d'état manuel d'un matériel (machine à états {@link EtatMateriel}). */
@Service
@Transactional
public class ChangerEtatMaterielService implements ChangerEtatMaterielUseCase {

  private final MaterielRepository materiels;
  private final MouvementRepository mouvements;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;
  private final SuiviEventPublisher evenements;
  private final Clock clock;

  /**
   * @param materiels stockage des matériels
   * @param mouvements journal des mouvements
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   * @param evenements publication des événements
   * @param clock horloge
   */
  public ChangerEtatMaterielService(
      MaterielRepository materiels,
      MouvementRepository mouvements,
      ChargeurAgregats chargeur,
      AssembleurVues vues,
      SuiviEventPublisher evenements,
      Clock clock) {
    this.materiels = materiels;
    this.mouvements = mouvements;
    this.chargeur = chargeur;
    this.vues = vues;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public MaterielVue changerEtat(
      UUID materielId, EtatMateriel etat, String motif, CurrentUser auteur) {
    Materiel materiel = chargeur.materiel(materielId);
    EtatMateriel avant = materiel.getEtat();
    materiel.changerEtat(etat, chargeur.contexte(materiel));
    Materiel enregistre = materiels.save(materiel);
    Chantier chantier = chargeur.chantierOuNull(enregistre.getChantierActuelId());
    mouvements.ajouter(
        Mouvement.de(
            enregistre.getId(),
            TypeMouvement.MAINTENANCE,
            null,
            chantier,
            Instant.now(clock),
            Auteurs.de(auteur),
            avant + " → " + enregistre.getEtat() + " : " + motif));
    evenements.materiel(EventTypes.MATERIEL_ETAT_CHANGE, enregistre, chantier, motif);
    return vues.materiel(enregistre);
  }
}
