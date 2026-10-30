package sn.cse.suivi.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.port.in.ReleverHeuresUseCase;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Materiel;

/** Relevé du compteur horaire (chef de chantier sur ses chantiers, ou Responsable Matériel). */
@Service
@Transactional
public class ReleverHeuresService implements ReleverHeuresUseCase {

  private final MaterielRepository materiels;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;

  /**
   * @param materiels stockage des matériels
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   */
  public ReleverHeuresService(
      MaterielRepository materiels, ChargeurAgregats chargeur, AssembleurVues vues) {
    this.materiels = materiels;
    this.chargeur = chargeur;
    this.vues = vues;
  }

  @Override
  public MaterielVue relever(UUID materielId, long heures, CurrentUser auteur) {
    Materiel materiel = chargeur.materiel(materielId);
    if (auteur.estRestreintAuxChantiers()
        && (materiel.getChantierActuelId() == null
            || !auteur.peutAccederAuChantier(materiel.getChantierActuelId()))) {
      throw SuiviErreurs.chantierNonAutorise(materiel.getChantierActuelId());
    }
    materiel.releverHeures(heures);
    return vues.materiel(materiels.save(materiel));
  }
}
