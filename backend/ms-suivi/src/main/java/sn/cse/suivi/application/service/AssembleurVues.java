package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.application.query.MaintenanceVue;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.application.query.PanneVue;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.TypeEngin;

/**
 * Construit les vues de lecture en résolvant les libellés (chantier, matériel, type d'engin) par
 * lots, afin d'éviter une requête par ligne.
 */
@Component
public class AssembleurVues {

  private final ChantierRepository chantiers;
  private final MaterielRepository materiels;
  private final TypeEnginRepository types;
  private final Clock clock;

  /**
   * @param chantiers stockage des chantiers
   * @param materiels stockage des matériels
   * @param types stockage des types d'engins
   * @param clock horloge
   */
  public AssembleurVues(
      ChantierRepository chantiers,
      MaterielRepository materiels,
      TypeEnginRepository types,
      Clock clock) {
    this.chantiers = chantiers;
    this.materiels = materiels;
    this.types = types;
    this.clock = clock;
  }

  /**
   * @param liste chantiers
   * @return vues avec le nombre de matériels présents
   */
  public List<ChantierVue> chantiers(List<Chantier> liste) {
    Map<UUID, Long> compte = materiels.compterParChantier(ids(liste, Chantier::getId));
    return liste.stream().map(c -> new ChantierVue(c, compte.getOrDefault(c.getId(), 0L))).toList();
  }

  /**
   * @param chantier chantier
   * @return sa vue
   */
  public ChantierVue chantier(Chantier chantier) {
    return chantiers(List.of(chantier)).get(0);
  }

  /**
   * @param liste matériels
   * @return vues avec libellés et échéance de maintenance
   */
  public List<MaterielVue> materiels(List<Materiel> liste) {
    LocalDate aujourdhui = LocalDate.now(clock);
    Map<UUID, Chantier> parChantier = chantiersParId(ids(liste, Materiel::getChantierActuelId));
    Map<UUID, String> libelles =
        types.findAll().stream()
            .collect(Collectors.toMap(TypeEngin::getId, TypeEngin::getLibelle, (a, b) -> a));
    return liste.stream()
        .map(
            m ->
                new MaterielVue(
                    m,
                    m.getDescription().typeEnginId() == null
                        ? null
                        : libelles.get(m.getDescription().typeEnginId()),
                    nom(parChantier.get(m.getChantierActuelId())),
                    m.estMaintenanceDue(aujourdhui)))
        .toList();
  }

  /**
   * @param materiel matériel
   * @return sa vue
   */
  public MaterielVue materiel(Materiel materiel) {
    return materiels(List.of(materiel)).get(0);
  }

  /**
   * @param liste affectations
   * @return vues avec libellés
   */
  public List<AffectationVue> affectations(List<Affectation> liste) {
    Map<UUID, Materiel> parMateriel = materielsParId(ids(liste, Affectation::getMaterielId));
    Map<UUID, Chantier> parChantier = chantiersParId(ids(liste, Affectation::getChantierId));
    return liste.stream()
        .map(
            a -> {
              Materiel m = parMateriel.get(a.getMaterielId());
              return new AffectationVue(
                  a,
                  m == null ? null : m.getCode(),
                  m == null ? null : m.getDesignation(),
                  m == null ? null : m.getCategorie(),
                  nom(parChantier.get(a.getChantierId())));
            })
        .toList();
  }

  /**
   * @param affectation affectation
   * @param materiel son matériel
   * @param chantier son chantier
   * @return sa vue (sans requête supplémentaire)
   */
  public AffectationVue affectation(Affectation affectation, Materiel materiel, Chantier chantier) {
    return new AffectationVue(
        affectation,
        materiel.getCode(),
        materiel.getDesignation(),
        materiel.getCategorie(),
        chantier.getNom());
  }

  /**
   * @param liste ordres de travail
   * @return vues avec libellés
   */
  public List<MaintenanceVue> maintenances(List<Maintenance> liste) {
    Map<UUID, Materiel> parMateriel = materielsParId(ids(liste, Maintenance::getMaterielId));
    Map<UUID, Chantier> parChantier = chantiersParId(ids(liste, Maintenance::getChantierId));
    return liste.stream()
        .map(
            mt -> {
              Materiel m = parMateriel.get(mt.getMaterielId());
              return new MaintenanceVue(
                  mt,
                  m == null ? null : m.getCode(),
                  m == null ? null : m.getDesignation(),
                  nom(parChantier.get(mt.getChantierId())));
            })
        .toList();
  }

  /**
   * @param maintenance ordre de travail
   * @return sa vue
   */
  public MaintenanceVue maintenance(Maintenance maintenance) {
    return maintenances(List.of(maintenance)).get(0);
  }

  /**
   * @param liste pannes
   * @return vues avec libellés
   */
  public List<PanneVue> pannes(List<Panne> liste) {
    Map<UUID, Materiel> parMateriel = materielsParId(ids(liste, Panne::getMaterielId));
    Map<UUID, Chantier> parChantier = chantiersParId(ids(liste, Panne::getChantierId));
    return liste.stream()
        .map(
            p -> {
              Materiel m = parMateriel.get(p.getMaterielId());
              return new PanneVue(
                  p,
                  m == null ? null : m.getCode(),
                  m == null ? null : m.getDesignation(),
                  nom(parChantier.get(p.getChantierId())));
            })
        .toList();
  }

  /**
   * Remplace le contenu d'une page en conservant ses métadonnées.
   *
   * @param page page source
   * @param conversion conversion par lot du contenu
   * @param <T> type source
   * @param <R> type cible
   * @return la page convertie
   */
  public static <T, R> PageResponse<R> page(
      PageResponse<T> page, Function<List<T>, List<R>> conversion) {
    return new PageResponse<>(
        conversion.apply(page.content()),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages(),
        page.first(),
        page.last());
  }

  private Map<UUID, Chantier> chantiersParId(Collection<UUID> ids) {
    // Collections.emptyMap tolère get(null) (matériel sans chantier), contrairement à Map.of.
    return ids.isEmpty()
        ? Collections.emptyMap()
        : chantiers.findAllById(ids).stream()
            .collect(Collectors.toMap(Chantier::getId, Function.identity()));
  }

  private Map<UUID, Materiel> materielsParId(Collection<UUID> ids) {
    return ids.isEmpty()
        ? Collections.emptyMap()
        : materiels.findAllById(ids).stream()
            .collect(Collectors.toMap(Materiel::getId, Function.identity()));
  }

  private static <T> List<UUID> ids(List<T> liste, Function<T, UUID> id) {
    return liste.stream().map(id).filter(Objects::nonNull).distinct().toList();
  }

  private static String nom(Chantier chantier) {
    return chantier == null ? null : chantier.getNom();
  }
}
