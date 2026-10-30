package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.in.ConsulterMaterielsUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.query.FiltreMateriels;
import sn.cse.suivi.application.query.MaterielDetailVue;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Materiel;

/** Consultation du parc (liste filtrée et fiche détaillée). */
@Service
@Transactional(readOnly = true)
public class ConsulterMaterielsService implements ConsulterMaterielsUseCase {

  private final MaterielRepository materiels;
  private final AffectationRepository affectations;
  private final MaintenanceRepository maintenances;
  private final PanneRepository pannes;
  private final MouvementRepository mouvements;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;
  private final SuiviParametres parametres;
  private final Clock clock;

  /**
   * @param materiels stockage des matériels
   * @param affectations stockage des affectations
   * @param maintenances stockage des ordres de travail
   * @param pannes stockage des pannes
   * @param mouvements journal des mouvements
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public ConsulterMaterielsService(
      MaterielRepository materiels,
      AffectationRepository affectations,
      MaintenanceRepository maintenances,
      PanneRepository pannes,
      MouvementRepository mouvements,
      ChargeurAgregats chargeur,
      AssembleurVues vues,
      SuiviParametres parametres,
      Clock clock) {
    this.materiels = materiels;
    this.affectations = affectations;
    this.maintenances = maintenances;
    this.pannes = pannes;
    this.mouvements = mouvements;
    this.chargeur = chargeur;
    this.vues = vues;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public PageResponse<MaterielVue> rechercher(
      Criteres criteres, PageQuery page, CurrentUser lecteur) {
    FiltreMateriels filtre =
        new FiltreMateriels(
            criteres.etat(),
            criteres.categorie(),
            criteres.q(),
            criteres.maintenanceDue(),
            Perimetre.de(lecteur).restreindreA(criteres.chantierId()));
    return AssembleurVues.page(
        materiels.rechercher(filtre, page, LocalDate.now(clock)), vues::materiels);
  }

  @Override
  public MaterielDetailVue detail(UUID id, CurrentUser lecteur) {
    Materiel materiel = chargeur.materiel(id);
    if (lecteur.estRestreintAuxChantiers()
        && (materiel.getChantierActuelId() == null
            || !lecteur.peutAccederAuChantier(materiel.getChantierActuelId()))) {
      throw SuiviErreurs.chantierNonAutorise(materiel.getChantierActuelId());
    }
    int taille = parametres.historique().taille();
    return new MaterielDetailVue(
        vues.materiel(materiel),
        vues.affectations(affectations.findRecentesByMateriel(id, taille)),
        vues.maintenances(maintenances.findRecentesByMateriel(id, taille)),
        vues.pannes(pannes.findRecentesByMateriel(id, taille)),
        mouvements.findRecentsByMateriel(id, taille));
  }
}
