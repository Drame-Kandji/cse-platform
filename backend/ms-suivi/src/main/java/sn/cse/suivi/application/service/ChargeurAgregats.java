package sn.cse.suivi.application.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.ContexteMateriel;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.TypeEngin;

/** Chargement des agrégats par identifiant, avec erreur 404 explicite. */
@Component
public class ChargeurAgregats {

  private final ChantierRepository chantiers;
  private final MaterielRepository materiels;
  private final AffectationRepository affectations;
  private final MaintenanceRepository maintenances;
  private final PanneRepository pannes;
  private final TypeEnginRepository types;

  /**
   * @param chantiers stockage des chantiers
   * @param materiels stockage des matériels
   * @param affectations stockage des affectations
   * @param maintenances stockage des ordres de travail
   * @param pannes stockage des pannes
   * @param types stockage des types d'engins
   */
  public ChargeurAgregats(
      ChantierRepository chantiers,
      MaterielRepository materiels,
      AffectationRepository affectations,
      MaintenanceRepository maintenances,
      PanneRepository pannes,
      TypeEnginRepository types) {
    this.chantiers = chantiers;
    this.materiels = materiels;
    this.affectations = affectations;
    this.maintenances = maintenances;
    this.pannes = pannes;
    this.types = types;
  }

  /**
   * @param id identifiant
   * @return le chantier
   */
  public Chantier chantier(UUID id) {
    return chantiers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chantier", id));
  }

  /**
   * @param id identifiant, peut être {@code null}
   * @return le chantier, ou {@code null} si {@code id} est nul ou inconnu
   */
  public Chantier chantierOuNull(UUID id) {
    return id == null ? null : chantiers.findById(id).orElse(null);
  }

  /**
   * @param id identifiant
   * @return le matériel
   */
  public Materiel materiel(UUID id) {
    return materiels.findById(id).orElseThrow(() -> new ResourceNotFoundException("Materiel", id));
  }

  /**
   * @param id identifiant
   * @return l'affectation
   */
  public Affectation affectation(UUID id) {
    return affectations
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Affectation", id));
  }

  /**
   * @param id identifiant
   * @return l'ordre de travail
   */
  public Maintenance maintenance(UUID id) {
    return maintenances
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Maintenance", id));
  }

  /**
   * @param id identifiant
   * @return la panne
   */
  public Panne panne(UUID id) {
    return pannes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Panne", id));
  }

  /**
   * @param materiel matériel
   * @return sa situation opérationnelle (affectation active, maintenance en cours)
   */
  public ContexteMateriel contexte(Materiel materiel) {
    return new ContexteMateriel(
        affectations.findActiveByMateriel(materiel.getId()).isPresent(),
        maintenances.existeEnCours(materiel.getId(), null));
  }

  /**
   * @param id identifiant, peut être {@code null}
   * @return le type d'engin, ou {@code null} si {@code id} est nul
   */
  public TypeEngin typeEnginOuNull(UUID id) {
    return id == null
        ? null
        : types.findById(id).orElseThrow(() -> new ResourceNotFoundException("TypeEngin", id));
  }
}
