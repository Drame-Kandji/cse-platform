package sn.cse.suivi.application.fake;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.GenerateurSequence;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.application.query.FiltreAffectations;
import sn.cse.suivi.application.query.FiltreChantiers;
import sn.cse.suivi.application.query.FiltreMaintenances;
import sn.cse.suivi.application.query.FiltreMateriels;
import sn.cse.suivi.application.query.FiltrePannes;
import sn.cse.suivi.application.query.FiltrePlanning;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.application.service.AssembleurVues;
import sn.cse.suivi.application.service.ChargeurAgregats;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeEngin;

/** Ensemble cohérent de ports en mémoire pour tester les cas d'utilisation sans base. */
public final class Memoire {

  /** Date du jour des tests. */
  public static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 30);

  /** Horloge figée au 30/10/2026 09:00 UTC. */
  public final Clock clock =
      Clock.fixed(AUJOURDHUI.atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  /** Paramètres de référence (valeurs de application.yml). */
  public final SuiviParametres parametres =
      new SuiviParametres(
          new SuiviParametres.Alertes(false, "0 0 6 * * *", "0 15 6 * * *", 3, 7),
          new SuiviParametres.Historique(20),
          new SuiviParametres.Disponibilite(50),
          new SuiviParametres.Planning(366, 30, 90),
          new SuiviParametres.Livraison(180, new BigDecimal("0.002")),
          new SuiviParametres.Numerotation(
              "OT",
              Map.of(
                  CategorieMateriel.MATERIEL_ROULANT, "ENG",
                  CategorieMateriel.OUTILLAGE, "OUT",
                  CategorieMateriel.EQUIPEMENT_INFORMATIQUE, "INF",
                  CategorieMateriel.CONSOMMABLE, "CSM")));

  public final Chantiers chantiers = new Chantiers();
  public final Materiels materiels = new Materiels();
  public final Affectations affectations = new Affectations();
  public final Maintenances maintenances = new Maintenances();
  public final Pannes pannes = new Pannes();
  public final Mouvements mouvements = new Mouvements();
  public final Types types = new Types();
  public final Sequences sequences = new Sequences();
  public final Evenements evenements = new Evenements();
  public final ChargeurAgregats chargeur =
      new ChargeurAgregats(chantiers, materiels, affectations, maintenances, pannes, types);
  public final AssembleurVues vues = new AssembleurVues(chantiers, materiels, types, clock);

  /** Chantiers en mémoire. */
  public static final class Chantiers implements ChantierRepository {
    private final Stock<Chantier, Chantier.Snapshot> stock =
        new Stock<>(
            Chantier::snapshot,
            Chantier::reconstituer,
            Chantier.Snapshot::id,
            Chantier.Snapshot::version,
            (s, v) -> new Chantier.Snapshot(s.id(), s.donnees(), s.statut(), v));

    @Override
    public Optional<Chantier> findById(UUID id) {
      return stock.findById(id);
    }

    @Override
    public Chantier save(Chantier chantier) {
      return stock.save(chantier);
    }

    @Override
    public boolean existsByCode(String code, UUID exclureId) {
      return !stock
          .filtrer(c -> c.getCode().equals(code) && !c.getId().equals(exclureId))
          .isEmpty();
    }

    @Override
    public PageResponse<Chantier> rechercher(FiltreChantiers f, PageQuery page) {
      return Stock.page(
          stock.filtrer(
              c ->
                  (f.statut() == null || c.getStatut() == f.statut())
                      && contient(f.q(), c.getCode(), c.getNom(), c.getRegion())
                      && Stock.visible(f.perimetre(), c.getId())),
          page);
    }

    @Override
    public List<Chantier> findAllById(Collection<UUID> ids) {
      return stock.filtrer(c -> ids.contains(c.getId()));
    }

    @Override
    public List<Chantier> findAll() {
      return stock.tous();
    }
  }

  /** Matériels en mémoire. */
  public static final class Materiels implements MaterielRepository {
    private final Stock<Materiel, Materiel.Snapshot> stock =
        new Stock<>(
            Materiel::snapshot,
            Materiel::reconstituer,
            Materiel.Snapshot::id,
            Materiel.Snapshot::version,
            Memoire::versionner);

    @Override
    public Optional<Materiel> findById(UUID id) {
      return stock.findById(id);
    }

    @Override
    public Materiel save(Materiel materiel) {
      return stock.save(materiel);
    }

    @Override
    public boolean existsByCode(String code, UUID exclureId) {
      return !stock
          .filtrer(m -> m.getCode().equals(code) && !m.getId().equals(exclureId))
          .isEmpty();
    }

    @Override
    public PageResponse<Materiel> rechercher(FiltreMateriels f, PageQuery page, LocalDate jour) {
      return Stock.page(
          stock.filtrer(
              m ->
                  (f.etat() == null || m.getEtat() == f.etat())
                      && (f.categorie() == null || m.getCategorie() == f.categorie())
                      && contient(f.q(), m.getCode(), m.getDesignation())
                      && (f.maintenanceDue() == null
                          || m.estMaintenanceDue(jour) == f.maintenanceDue())
                      && Stock.visible(f.perimetre(), m.getChantierActuelId())),
          page);
    }

    @Override
    public List<Materiel> findAllById(Collection<UUID> ids) {
      return stock.filtrer(m -> ids.contains(m.getId()));
    }

    @Override
    public List<Materiel> rechercherDisponibles(
        CategorieMateriel categorie, String designation, int limite) {
      return disponibles(categorie, designation).stream().limit(limite).toList();
    }

    @Override
    public long compterDisponibles(CategorieMateriel categorie, String designation) {
      return disponibles(categorie, designation).size();
    }

    private List<Materiel> disponibles(CategorieMateriel categorie, String designation) {
      return stock.filtrer(
          m ->
              m.getEtat() == EtatMateriel.DISPONIBLE
                  && !m.isReformeDemandee()
                  && (categorie == null || m.getCategorie() == categorie)
                  && contient(designation, m.getDesignation()));
    }

    @Override
    public List<Materiel> findByTypeEngin(UUID typeEnginId) {
      return stock.filtrer(m -> typeEnginId.equals(m.getDescription().typeEnginId()));
    }

    @Override
    public List<Materiel> findMaintenanceDue(LocalDate aujourdhui) {
      return stock.filtrer(m -> m.estMaintenanceDue(aujourdhui));
    }

    @Override
    public List<Materiel> findPourCarte(
        CategorieMateriel categorie, EtatMateriel etat, Perimetre perimetre) {
      return stock.filtrer(
          m ->
              m.getPosition() != null
                  && (etat == null ? m.getEtat() != EtatMateriel.REFORME : m.getEtat() == etat)
                  && (categorie == null || m.getCategorie() == categorie)
                  && Stock.visible(perimetre, m.getChantierActuelId()));
    }

    @Override
    public Map<UUID, Long> compterParChantier(Collection<UUID> chantierIds) {
      Map<UUID, Long> compte = new HashMap<>();
      stock
          .filtrer(m -> chantierIds.contains(m.getChantierActuelId()))
          .forEach(m -> compte.merge(m.getChantierActuelId(), 1L, Long::sum));
      return compte;
    }
  }

  /** Affectations en mémoire (avec l'invariant « une affectation active par matériel »). */
  public static final class Affectations implements AffectationRepository {
    private final Stock<Affectation, Affectation.Snapshot> stock =
        new Stock<>(
            Affectation::snapshot,
            Affectation::reconstituer,
            Affectation.Snapshot::id,
            Affectation.Snapshot::version,
            Memoire::versionner);

    @Override
    public Affectation save(Affectation affectation) {
      boolean autreActive =
          affectation.estActive()
              && stock
                      .filtrer(
                          a ->
                              a.estActive()
                                  && a.getMaterielId().equals(affectation.getMaterielId())
                                  && !a.getId().equals(affectation.getId()))
                      .size()
                  > 0;
      if (autreActive) {
        throw SuiviErreurs.conflitAffectation("doublon");
      }
      return stock.save(affectation);
    }

    @Override
    public Optional<Affectation> findById(UUID id) {
      return stock.findById(id);
    }

    @Override
    public List<Affectation> findActivesByMateriel(UUID materielId) {
      return stock.filtrer(a -> a.estActive() && a.getMaterielId().equals(materielId));
    }

    @Override
    public List<Affectation> findRecentesByMateriel(UUID materielId, int limite) {
      return stock.filtrer(a -> a.getMaterielId().equals(materielId)).stream()
          .sorted(Comparator.comparing(Affectation::getDateDebut).reversed())
          .limit(limite)
          .toList();
    }

    @Override
    public PageResponse<Affectation> rechercher(FiltreAffectations f, PageQuery page) {
      return Stock.page(
          stock.filtrer(
              a ->
                  (f.materielId() == null || a.getMaterielId().equals(f.materielId()))
                      && (f.statut() == null || a.getStatut() == f.statut())
                      && Stock.visible(f.perimetre(), a.getChantierId())),
          page);
    }

    @Override
    public List<Affectation> planning(FiltrePlanning f) {
      return stock.filtrer(
          a ->
              a.periodeEffective().chevauche(f.periode())
                  && Stock.visible(f.perimetre(), a.getChantierId()));
    }

    @Override
    public List<Affectation> findActivesFinissantAuPlusTard(LocalDate limite) {
      return stock.filtrer(a -> a.estActive() && !a.getDateFinPrevue().isAfter(limite));
    }

    @Override
    public long compterActivesParChantier(UUID chantierId) {
      return stock.filtrer(a -> a.estActive() && a.getChantierId().equals(chantierId)).size();
    }
  }

  /** Ordres de travail en mémoire. */
  public static final class Maintenances implements MaintenanceRepository {
    private final Stock<Maintenance, Maintenance.Snapshot> stock =
        new Stock<>(
            Maintenance::snapshot,
            Maintenance::reconstituer,
            Maintenance.Snapshot::id,
            Maintenance.Snapshot::version,
            Memoire::versionner);

    @Override
    public Maintenance save(Maintenance maintenance) {
      return stock.save(maintenance);
    }

    @Override
    public Optional<Maintenance> findById(UUID id) {
      return stock.findById(id);
    }

    @Override
    public PageResponse<Maintenance> rechercher(FiltreMaintenances f, PageQuery page) {
      return Stock.page(
          stock.filtrer(
              m ->
                  (f.statut() == null || m.getStatut() == f.statut())
                      && (f.type() == null || m.getType() == f.type())
                      && (f.materielId() == null || m.getMaterielId().equals(f.materielId()))
                      && Stock.visible(f.perimetre(), m.getChantierId())),
          page);
    }

    @Override
    public List<Maintenance> findRecentesByMateriel(UUID materielId, int limite) {
      return stock.filtrer(m -> m.getMaterielId().equals(materielId)).stream()
          .limit(limite)
          .toList();
    }

    @Override
    public boolean existeEnCours(UUID materielId, UUID exclureId) {
      return !stock
          .filtrer(
              m ->
                  m.getMaterielId().equals(materielId)
                      && m.getStatut() == StatutMaintenance.EN_COURS
                      && !m.getId().equals(exclureId))
          .isEmpty();
    }

    @Override
    public boolean existeOuverte(UUID materielId) {
      return !stock
          .filtrer(
              m ->
                  m.getMaterielId().equals(materielId)
                      && m.getStatut() != StatutMaintenance.CLOTUREE)
          .isEmpty();
    }

    @Override
    public Set<UUID> materielsAvecMaintenancePlanifiee(
        Collection<UUID> ids, LocalDate du, LocalDate au) {
      Set<UUID> resultat = new HashSet<>();
      stock
          .filtrer(
              m ->
                  ids.contains(m.getMaterielId())
                      && m.getStatut() == StatutMaintenance.PLANIFIEE
                      && !m.getDatePlanifiee().isBefore(du)
                      && !m.getDatePlanifiee().isAfter(au))
          .forEach(m -> resultat.add(m.getMaterielId()));
      return resultat;
    }
  }

  /** Pannes en mémoire. */
  public static final class Pannes implements PanneRepository {
    private final Stock<Panne, Panne.Snapshot> stock =
        new Stock<>(
            Panne::snapshot,
            Panne::reconstituer,
            Panne.Snapshot::id,
            Panne.Snapshot::version,
            (s, v) ->
                new Panne.Snapshot(
                    s.id(),
                    s.materielId(),
                    s.chantierId(),
                    s.gravite(),
                    s.description(),
                    s.statut(),
                    s.signaleParId(),
                    s.signaleParNom(),
                    s.dateSignalement(),
                    s.maintenanceId(),
                    v));

    @Override
    public Panne save(Panne panne) {
      return stock.save(panne);
    }

    @Override
    public Optional<Panne> findById(UUID id) {
      return stock.findById(id);
    }

    @Override
    public PageResponse<Panne> rechercher(FiltrePannes f, PageQuery page) {
      return Stock.page(
          stock.filtrer(
              p ->
                  (f.statut() == null || p.getStatut() == f.statut())
                      && (f.materielId() == null || p.getMaterielId().equals(f.materielId()))
                      && Stock.visible(f.perimetre(), p.getChantierId())),
          page);
    }

    @Override
    public List<Panne> findRecentesByMateriel(UUID materielId, int limite) {
      return stock.filtrer(p -> p.getMaterielId().equals(materielId)).stream()
          .limit(limite)
          .toList();
    }
  }

  /** Journal des mouvements en mémoire. */
  public static final class Mouvements implements MouvementRepository {
    public final List<Mouvement> journal = new ArrayList<>();

    @Override
    public void ajouter(Mouvement mouvement) {
      journal.add(mouvement);
    }

    @Override
    public List<Mouvement> findRecentsByMateriel(UUID materielId, int limite) {
      return journal.stream()
          .filter(m -> m.materielId().equals(materielId))
          .sorted(Comparator.comparing(Mouvement::date).reversed())
          .limit(limite)
          .toList();
    }
  }

  /** Types d'engins en mémoire. */
  public static final class Types implements TypeEnginRepository {
    private final Stock<TypeEngin, TypeEngin.Snapshot> stock =
        new Stock<>(
            TypeEngin::snapshot,
            TypeEngin::reconstituer,
            TypeEngin.Snapshot::id,
            TypeEngin.Snapshot::version,
            (s, v) -> new TypeEngin.Snapshot(s.id(), s.donnees(), v));

    @Override
    public List<TypeEngin> findAll() {
      return stock.tous().stream().sorted(Comparator.comparing(TypeEngin::getLibelle)).toList();
    }

    @Override
    public Optional<TypeEngin> findById(UUID id) {
      return stock.findById(id);
    }

    @Override
    public TypeEngin save(TypeEngin type) {
      return stock.save(type);
    }

    @Override
    public boolean existsByCode(String code, UUID exclureId) {
      return !stock
          .filtrer(t -> t.getCode().equals(code) && !t.getId().equals(exclureId))
          .isEmpty();
    }
  }

  /** Séquences annuelles en mémoire. */
  public static final class Sequences implements GenerateurSequence {
    private final Map<String, Long> valeurs = new HashMap<>();

    @Override
    public long prochaineValeur(String serie, int annee) {
      return valeurs.merge(serie + annee, 1L, Long::sum);
    }
  }

  /**
   * Événement publié.
   *
   * @param type type
   * @param agregatId identifiant de l'agrégat
   * @param motif motif éventuel
   */
  public record Evenement(String type, UUID agregatId, String motif) {}

  /** Publication enregistrée en mémoire. */
  public static final class Evenements implements SuiviEventPublisher {
    public final List<Evenement> publies = new ArrayList<>();

    @Override
    public void chantier(String type, Chantier chantier) {
      publies.add(new Evenement(type, chantier.getId(), null));
    }

    @Override
    public void materiel(String type, Materiel materiel, Chantier chantierActuel, String motif) {
      publies.add(new Evenement(type, materiel.getId(), motif));
    }

    @Override
    public void affectation(String type, Affectation a, Materiel m, Chantier c) {
      publies.add(new Evenement(type, a.getId(), null));
    }

    @Override
    public void maintenance(String type, Maintenance maintenance, Materiel materiel) {
      publies.add(new Evenement(type, maintenance.getId(), null));
    }

    @Override
    public void panneSignalee(Panne panne, Materiel materiel, Chantier chantier) {
      publies.add(new Evenement("PANNE_SIGNALEE", panne.getId(), null));
    }

    /**
     * @return types publiés dans l'ordre
     */
    public List<String> types() {
      return publies.stream().map(Evenement::type).toList();
    }
  }

  static Materiel.Snapshot versionner(Materiel.Snapshot s, Long v) {
    return new Materiel.Snapshot(
        s.id(),
        s.description(),
        s.etat(),
        s.heuresFonctionnement(),
        s.heuresDerniereMaintenance(),
        s.dateReferenceMaintenance(),
        s.seuilMaintenanceHeures(),
        s.seuilMaintenanceJours(),
        s.dateDerniereAlerteMaintenance(),
        s.chantierActuelId(),
        s.position(),
        s.fournisseurId(),
        s.contratId(),
        s.reformeDemandee(),
        s.motifReforme(),
        v);
  }

  static Affectation.Snapshot versionner(Affectation.Snapshot s, Long v) {
    return new Affectation.Snapshot(
        s.id(),
        s.materielId(),
        s.chantierId(),
        s.besoinId(),
        s.periodePrevue(),
        s.dateFinReelle(),
        s.statut(),
        s.dateReception(),
        s.observationsReception(),
        s.commentaireLiberation(),
        s.fournisseurId(),
        s.contratId(),
        s.alerteFinEnvoyee(),
        v);
  }

  static Maintenance.Snapshot versionner(Maintenance.Snapshot s, Long v) {
    return new Maintenance.Snapshot(
        s.id(),
        s.numeroOt(),
        s.materielId(),
        s.chantierId(),
        s.type(),
        s.statut(),
        s.description(),
        s.datePlanifiee(),
        s.dateDebut(),
        s.dateFin(),
        s.technicien(),
        s.cout(),
        s.heuresCompteur(),
        s.observations(),
        s.panneId(),
        v);
  }

  private static boolean contient(String q, String... valeurs) {
    if (q == null || q.isBlank()) {
      return true;
    }
    String motif = q.toLowerCase(Locale.ROOT);
    for (String v : valeurs) {
      if (v != null && v.toLowerCase(Locale.ROOT).contains(motif)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return l'instant courant de l'horloge de test
   */
  public Instant maintenant() {
    return Instant.now(clock);
  }
}
