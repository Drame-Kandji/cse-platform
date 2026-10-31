package sn.cse.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.reporting.application.port.out.LectureReportingRepository;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.HistoriqueParc;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
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

/** Fausse persistance en mémoire, fidèle aux contrats des ports (upsert par identifiant). */
class ReportingEnMemoire implements ProjectionRepository, LectureReportingRepository {

  final Map<UUID, ChantierVue> chantiers = new LinkedHashMap<>();
  final Map<UUID, MaterielVue> materiels = new LinkedHashMap<>();
  final Map<UUID, AffectationVue> affectations = new LinkedHashMap<>();
  final Map<UUID, MaintenanceVue> maintenances = new LinkedHashMap<>();
  final Map<UUID, PanneVue> pannes = new LinkedHashMap<>();
  final Map<UUID, BesoinVue> besoins = new LinkedHashMap<>();
  final Map<UUID, AppelOffreVue> appelsOffres = new LinkedHashMap<>();
  final Map<UUID, ContratVue> contrats = new LinkedHashMap<>();
  final Map<UUID, FactureVue> factures = new LinkedHashMap<>();
  final Map<UUID, FournisseurVue> fournisseurs = new LinkedHashMap<>();
  final Map<UUID, LitigeVue> litiges = new LinkedHashMap<>();
  final Map<LocalDate, List<ParcInstantane>> instantanes = new LinkedHashMap<>();

  @Override
  public Optional<ChantierVue> chantier(UUID id) {
    return Optional.ofNullable(chantiers.get(id));
  }

  @Override
  public void enregistrer(ChantierVue vue) {
    chantiers.put(vue.id(), vue);
  }

  @Override
  public void referencer(ChantierVue reference) {
    chantiers.merge(reference.id(), reference, ChantierVue::fusionnerAvec);
  }

  @Override
  public Optional<MaterielVue> materiel(UUID id) {
    return Optional.ofNullable(materiels.get(id));
  }

  @Override
  public void enregistrer(MaterielVue vue) {
    materiels.put(vue.id(), vue);
  }

  @Override
  public List<MaterielVue> materiels() {
    return List.copyOf(materiels.values());
  }

  @Override
  public Optional<AffectationVue> affectation(UUID id) {
    return Optional.ofNullable(affectations.get(id));
  }

  @Override
  public void enregistrer(AffectationVue vue) {
    affectations.put(vue.id(), vue);
  }

  @Override
  public Optional<MaintenanceVue> maintenance(UUID id) {
    return Optional.ofNullable(maintenances.get(id));
  }

  @Override
  public void enregistrer(MaintenanceVue vue) {
    maintenances.put(vue.id(), vue);
  }

  @Override
  public Optional<Instant> derniereClotureMaintenance(UUID materielId) {
    return maintenances.values().stream()
        .filter(m -> materielId.equals(m.materielId()))
        .map(MaintenanceVue::clotureeLe)
        .filter(Objects::nonNull)
        .max(Instant::compareTo);
  }

  @Override
  public Optional<PanneVue> panne(UUID id) {
    return Optional.ofNullable(pannes.get(id));
  }

  @Override
  public void enregistrer(PanneVue vue) {
    pannes.put(vue.id(), vue);
  }

  @Override
  public List<PanneVue> pannesOuvertes(UUID materielId) {
    return pannes.values().stream()
        .filter(p -> materielId.equals(p.materielId()) && p.ouverte())
        .toList();
  }

  @Override
  public Optional<BesoinVue> besoin(UUID id) {
    return Optional.ofNullable(besoins.get(id));
  }

  @Override
  public void enregistrer(BesoinVue vue) {
    besoins.put(vue.id(), vue);
  }

  @Override
  public Optional<AppelOffreVue> appelOffre(UUID id) {
    return Optional.ofNullable(appelsOffres.get(id));
  }

  @Override
  public void enregistrer(AppelOffreVue vue) {
    appelsOffres.put(vue.id(), vue);
  }

  @Override
  public Optional<ContratVue> contrat(UUID id) {
    return Optional.ofNullable(contrats.get(id));
  }

  @Override
  public void enregistrer(ContratVue vue) {
    contrats.put(vue.id(), vue);
  }

  @Override
  public Optional<FactureVue> facture(UUID id) {
    return Optional.ofNullable(factures.get(id));
  }

  @Override
  public void enregistrer(FactureVue vue) {
    factures.put(vue.id(), vue);
  }

  @Override
  public Optional<FournisseurVue> fournisseur(UUID id) {
    return Optional.ofNullable(fournisseurs.get(id));
  }

  @Override
  public void enregistrer(FournisseurVue vue) {
    fournisseurs.put(vue.id(), vue);
  }

  @Override
  public void referencer(FournisseurVue reference) {
    fournisseurs.merge(reference.id(), reference, FournisseurVue::fusionnerAvec);
  }

  @Override
  public Optional<LitigeVue> litige(UUID id) {
    return Optional.ofNullable(litiges.get(id));
  }

  @Override
  public void enregistrer(LitigeVue vue) {
    litiges.put(vue.id(), vue);
  }

  @Override
  public void remplacerInstantanes(LocalDate jour, List<ParcInstantane> lignes) {
    instantanes.put(jour, List.copyOf(lignes));
  }

  @Override
  public DonneesReporting charger(Periode periode, Perimetre perimetre) {
    return new DonneesReporting(
        new ArrayList<>(chantiers.values()),
        new ArrayList<>(materiels.values()),
        new ArrayList<>(affectations.values()),
        new ArrayList<>(maintenances.values()),
        new ArrayList<>(pannes.values()),
        new ArrayList<>(besoins.values()),
        new ArrayList<>(appelsOffres.values()),
        new ArrayList<>(contrats.values()),
        new ArrayList<>(factures.values()),
        new ArrayList<>(fournisseurs.values()),
        new ArrayList<>(litiges.values()));
  }

  @Override
  public List<ParcInstantane> instantanes(Periode periode) {
    return instantanes.values().stream().flatMap(List::stream).toList();
  }

  @Override
  public HistoriqueParc historiqueParc() {
    return new HistoriqueParc(
        new ArrayList<>(materiels.values()),
        new ArrayList<>(affectations.values()),
        new ArrayList<>(maintenances.values()));
  }
}
