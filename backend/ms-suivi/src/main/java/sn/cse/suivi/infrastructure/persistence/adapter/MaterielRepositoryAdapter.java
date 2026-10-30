package sn.cse.suivi.infrastructure.persistence.adapter;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.query.FiltreMateriels;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.MaterielJpaRepository;

/** Adaptateur JPA des matériels. */
@Component
public class MaterielRepositoryAdapter implements MaterielRepository {

  private static final String CHANTIER = "chantierActuelId";

  private final MaterielJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public MaterielRepositoryAdapter(MaterielJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Materiel> findById(UUID id) {
    return jpa.findById(id).map(PersistenceMapper::versDomaine);
  }

  @Override
  public Materiel save(Materiel materiel) {
    return PersistenceMapper.versDomaine(jpa.saveAndFlush(PersistenceMapper.versEntite(materiel)));
  }

  @Override
  public boolean existsByCode(String code, UUID exclureId) {
    return exclureId == null ? jpa.existsByCode(code) : jpa.existsByCodeAndIdNot(code, exclureId);
  }

  @Override
  public PageResponse<Materiel> rechercher(
      FiltreMateriels filtre, PageQuery page, LocalDate aujourdhui) {
    Specification<MaterielEntity> spec =
        Specs.tous(
            Specs.egal("etat", filtre.etat()),
            Specs.egal("categorie", filtre.categorie()),
            Specs.contient(filtre.q(), "code", "designation", "immatriculation", "marque"),
            Specs.perimetre(CHANTIER, filtre.perimetre()),
            maintenanceDue(filtre.maintenanceDue(), aujourdhui));
    return Specs.page(
        jpa.findAll(spec, PageableFactory.toPageable(page)), PersistenceMapper::versDomaine);
  }

  @Override
  public List<Materiel> findAllById(Collection<UUID> ids) {
    return jpa.findAllById(ids).stream().map(PersistenceMapper::versDomaine).toList();
  }

  @Override
  public List<Materiel> rechercherDisponibles(
      CategorieMateriel categorie, String designation, int limite) {
    return jpa
        .findAll(disponibles(categorie, designation), PageRequest.of(0, limite, Sort.by("code")))
        .stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public long compterDisponibles(CategorieMateriel categorie, String designation) {
    return jpa.count(disponibles(categorie, designation));
  }

  @Override
  public List<Materiel> findByTypeEngin(UUID typeEnginId) {
    return jpa.findByTypeEnginId(typeEnginId).stream().map(PersistenceMapper::versDomaine).toList();
  }

  @Override
  public List<Materiel> findMaintenanceDue(LocalDate aujourdhui) {
    return jpa.findAll(maintenanceDue(Boolean.TRUE, aujourdhui), Sort.by("code")).stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public List<Materiel> findPourCarte(
      CategorieMateriel categorie, EtatMateriel etat, Perimetre perimetre) {
    Specification<MaterielEntity> etatSpec =
        etat != null
            ? Specs.egal("etat", etat)
            : (root, q, cb) -> cb.notEqual(root.get("etat"), EtatMateriel.REFORME);
    Specification<MaterielEntity> spec =
        Specs.tous(
            etatSpec,
            Specs.egal("categorie", categorie),
            Specs.perimetre(CHANTIER, perimetre),
            (root, q, cb) ->
                cb.and(cb.isNotNull(root.get("latitude")), cb.isNotNull(root.get("longitude"))));
    return jpa.findAll(spec, Sort.by("code")).stream().map(PersistenceMapper::versDomaine).toList();
  }

  @Override
  public Map<UUID, Long> compterParChantier(Collection<UUID> chantierIds) {
    Map<UUID, Long> resultat = new HashMap<>();
    if (chantierIds.isEmpty()) {
      return resultat;
    }
    for (Object[] ligne : jpa.compterParChantier(chantierIds)) {
      resultat.put((UUID) ligne[0], (Long) ligne[1]);
    }
    return resultat;
  }

  private static Specification<MaterielEntity> disponibles(
      CategorieMateriel categorie, String designation) {
    return Specs.tous(
        Specs.egal("etat", EtatMateriel.DISPONIBLE),
        Specs.egal("reformeDemandee", false),
        Specs.egal("categorie", categorie),
        Specs.contient(designation, "designation"));
  }

  // Même règle que Materiel.estMaintenanceDue, exprimée en SQL pour filtrer et paginer en base.
  private static Specification<MaterielEntity> maintenanceDue(Boolean due, LocalDate aujourdhui) {
    return (root, q, cb) -> {
      if (due == null) {
        return null;
      }
      Expression<Long> depuis =
          cb.diff(
              root.<Long>get("heuresFonctionnement"), root.<Long>get("heuresDerniereMaintenance"));
      Expression<Integer> seuil = root.get("seuilMaintenanceHeures");
      Predicate heures = cb.and(cb.isNotNull(seuil), cb.ge(depuis, seuil));
      Expression<LocalDate> prochaine = root.get("prochaineMaintenance");
      // Les colonnes nulles sont neutralisées pour que la négation reste exacte (logique ternaire).
      Predicate date = cb.and(cb.isNotNull(prochaine), cb.lessThanOrEqualTo(prochaine, aujourdhui));
      Predicate atteinte =
          cb.and(cb.notEqual(root.get("etat"), EtatMateriel.REFORME), cb.or(heures, date));
      return due ? atteinte : cb.not(atteinte);
    };
  }
}
