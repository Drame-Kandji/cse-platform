package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.port.in.ReformerMaterielUseCase;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Réforme en deux temps (R13) : demande du Responsable Matériel, décision de la DG. */
@Service
@Transactional
public class ReformerMaterielService implements ReformerMaterielUseCase {

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
  public ReformerMaterielService(
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
  public MaterielVue demander(UUID materielId, String motif, CurrentUser auteur) {
    Materiel materiel = chargeur.materiel(materielId);
    materiel.demanderReforme(motif);
    Materiel enregistre = materiels.save(materiel);
    Chantier chantier = chargeur.chantierOuNull(enregistre.getChantierActuelId());
    tracer(enregistre, auteur, "Demande de réforme : " + motif);
    evenements.materiel(EventTypes.MATERIEL_REFORME_DEMANDEE, enregistre, chantier, motif);
    return vues.materiel(enregistre);
  }

  @Override
  public MaterielVue decider(
      UUID materielId, boolean approuve, String commentaire, CurrentUser auteur) {
    Materiel materiel = chargeur.materiel(materielId);
    String motifDemande = materiel.getMotifReforme();
    materiel.deciderReforme(approuve, chargeur.contexte(materiel));
    Materiel enregistre = materiels.save(materiel);
    String suffixe = commentaire == null || commentaire.isBlank() ? "" : " · " + commentaire;
    if (approuve) {
      tracer(enregistre, auteur, "Réforme validée par la Direction Générale" + suffixe);
      evenements.materiel(EventTypes.MATERIEL_REFORME, enregistre, null, motifDemande + suffixe);
    } else {
      tracer(enregistre, auteur, "Réforme refusée par la Direction Générale" + suffixe);
      // Pas de type dédié au refus dans le contrat : l'instantané (état inchangé) est republié.
      Chantier chantier = chargeur.chantierOuNull(enregistre.getChantierActuelId());
      evenements.materiel(
          EventTypes.MATERIEL_ETAT_CHANGE, enregistre, chantier, "Réforme refusée" + suffixe);
    }
    return vues.materiel(enregistre);
  }

  private void tracer(Materiel materiel, CurrentUser auteur, String commentaire) {
    mouvements.ajouter(
        Mouvement.de(
            materiel.getId(),
            TypeMouvement.REFORME,
            null,
            null,
            Instant.now(clock),
            Auteurs.de(auteur),
            commentaire));
  }
}
