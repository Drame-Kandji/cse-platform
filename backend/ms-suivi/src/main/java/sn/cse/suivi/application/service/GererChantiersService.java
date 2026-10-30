package sn.cse.suivi.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.port.in.GererChantiersUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.DonneesChantier;

/** Création, modification et clôture des chantiers. */
@Service
@Transactional
public class GererChantiersService implements GererChantiersUseCase {

  private final ChantierRepository chantiers;
  private final AffectationRepository affectations;
  private final ChargeurAgregats chargeur;
  private final AssembleurVues vues;
  private final SuiviEventPublisher evenements;

  /**
   * @param chantiers stockage des chantiers
   * @param affectations stockage des affectations
   * @param chargeur chargement des agrégats
   * @param vues assembleur de vues
   * @param evenements publication des événements
   */
  public GererChantiersService(
      ChantierRepository chantiers,
      AffectationRepository affectations,
      ChargeurAgregats chargeur,
      AssembleurVues vues,
      SuiviEventPublisher evenements) {
    this.chantiers = chantiers;
    this.affectations = affectations;
    this.chargeur = chargeur;
    this.vues = vues;
    this.evenements = evenements;
  }

  @Override
  public ChantierVue creer(DonneesChantier donnees, CurrentUser auteur) {
    verifierCodeLibre(donnees.code(), null);
    Chantier chantier = chantiers.save(Chantier.ouvrir(donnees));
    evenements.chantier(EventTypes.CHANTIER_CREE, chantier);
    return vues.chantier(chantier);
  }

  @Override
  public ChantierVue modifier(UUID id, DonneesChantier donnees, long version, CurrentUser auteur) {
    Chantier chantier = chargeur.chantier(id);
    verifierCodeLibre(donnees.code(), id);
    chantier.modifier(donnees, version);
    Chantier enregistre = chantiers.save(chantier);
    evenements.chantier(EventTypes.CHANTIER_MODIFIE, enregistre);
    return vues.chantier(enregistre);
  }

  @Override
  public ChantierVue terminer(UUID id, CurrentUser auteur) {
    Chantier chantier = chargeur.chantier(id);
    chantier.terminer(affectations.compterActivesParChantier(id));
    Chantier enregistre = chantiers.save(chantier);
    evenements.chantier(EventTypes.CHANTIER_MODIFIE, enregistre);
    return vues.chantier(enregistre);
  }

  private void verifierCodeLibre(String code, UUID exclureId) {
    if (chantiers.existsByCode(code, exclureId)) {
      throw SuiviErreurs.conflit(
          "CODE_DEJA_UTILISE", "Le code chantier " + code + " est déjà utilisé.");
    }
  }
}
