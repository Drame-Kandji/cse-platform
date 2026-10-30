package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.port.in.GererMaterielsUseCase;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Enregistrement et modification des fiches matériel. */
@Service
@Transactional
public class GererMaterielsService implements GererMaterielsUseCase {

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
  public GererMaterielsService(
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
  public MaterielVue enregistrer(
      DescriptionMateriel description, Long heuresFonctionnement, CurrentUser auteur) {
    verifierCodeLibre(description.code(), null);
    Materiel materiel =
        Materiel.enregistrer(
            description,
            chargeur.typeEnginOuNull(description.typeEnginId()),
            heuresFonctionnement == null ? 0L : heuresFonctionnement,
            Materiel.Origine.AUCUNE,
            LocalDate.now(clock));
    Materiel enregistre = materiels.save(materiel);
    mouvements.ajouter(
        Mouvement.de(
            enregistre.getId(),
            TypeMouvement.ENREGISTREMENT,
            null,
            null,
            Instant.now(clock),
            Auteurs.de(auteur),
            "Entrée dans le parc"));
    evenements.materiel(EventTypes.MATERIEL_ENREGISTRE, enregistre, null, null);
    return vues.materiel(enregistre);
  }

  @Override
  public MaterielVue modifier(
      UUID id, DescriptionMateriel description, long version, CurrentUser auteur) {
    Materiel materiel = chargeur.materiel(id);
    verifierCodeLibre(description.code(), id);
    materiel.modifier(description, chargeur.typeEnginOuNull(description.typeEnginId()), version);
    return vues.materiel(materiels.save(materiel));
  }

  private void verifierCodeLibre(String code, UUID exclureId) {
    if (materiels.existsByCode(code, exclureId)) {
      throw SuiviErreurs.conflit(
          "CODE_DEJA_UTILISE", "Le code matériel " + code + " est déjà utilisé.");
    }
  }
}
