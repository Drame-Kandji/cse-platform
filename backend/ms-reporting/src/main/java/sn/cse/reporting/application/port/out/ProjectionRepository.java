package sn.cse.reporting.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/**
 * Écriture des vues de reporting. {@code enregistrer} est une insertion ou un remplacement (upsert)
 * par identifiant.
 */
public interface ProjectionRepository {

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<ChantierVue> chantier(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(ChantierVue vue);

  /**
   * Crée atomiquement une référence de chantier si le chantier est inconnu, ou complète seulement
   * son nom manquant : ne remplace jamais une vue écrite entre-temps par un autre consommateur.
   *
   * @param reference référence incomplète
   */
  void referencer(ChantierVue reference);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<MaterielVue> materiel(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(MaterielVue vue);

  /**
   * @return tous les matériels connus
   */
  List<MaterielVue> materiels();

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<AffectationVue> affectation(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(AffectationVue vue);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<MaintenanceVue> maintenance(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(MaintenanceVue vue);

  /**
   * @param materielId matériel
   * @return instant de la dernière clôture de maintenance du matériel
   */
  Optional<Instant> derniereClotureMaintenance(UUID materielId);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<PanneVue> panne(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(PanneVue vue);

  /**
   * @param materielId matériel
   * @return pannes non résolues du matériel
   */
  List<PanneVue> pannesOuvertes(UUID materielId);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<BesoinVue> besoin(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(BesoinVue vue);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<AppelOffreVue> appelOffre(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(AppelOffreVue vue);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<ContratVue> contrat(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(ContratVue vue);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<FactureVue> facture(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(FactureVue vue);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<FournisseurVue> fournisseur(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(FournisseurVue vue);

  /**
   * Même règle que {@link #referencer(ChantierVue)} pour un fournisseur.
   *
   * @param reference référence incomplète
   */
  void referencer(FournisseurVue reference);

  /**
   * @param id identifiant
   * @return la vue, si connue
   */
  Optional<LitigeVue> litige(UUID id);

  /**
   * @param vue vue à enregistrer
   */
  void enregistrer(LitigeVue vue);

  /**
   * @param jour jour photographié
   * @param lignes nouvel instantané du jour (remplace l'existant)
   */
  void remplacerInstantanes(LocalDate jour, List<ParcInstantane> lignes);
}
