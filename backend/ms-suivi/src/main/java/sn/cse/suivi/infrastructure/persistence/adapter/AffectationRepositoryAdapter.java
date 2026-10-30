package sn.cse.suivi.infrastructure.persistence.adapter;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.query.FiltreAffectations;
import sn.cse.suivi.application.query.FiltrePlanning;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.StatutAffectation;
import sn.cse.suivi.infrastructure.persistence.entity.AffectationEntity;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.AffectationJpaRepository;

/**
 * Adaptateur JPA des affectations. L'index unique partiel {@code ux_affectations_materiel_active}
 * garantit en base qu'un matériel n'a qu'une affectation active : une écriture concurrente perdante
 * est traduite en {@code CONFLIT_AFFECTATION}.
 */
@Component
public class AffectationRepositoryAdapter implements AffectationRepository {

  /** Nom de l'index unique « une affectation active par matériel » (migration V1). */
  static final String INDEX_AFFECTATION_ACTIVE = "ux_affectations_materiel_active";

  private static final String CHANTIER = "chantierId";

  private final AffectationJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public AffectationRepositoryAdapter(AffectationJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Affectation save(Affectation affectation) {
    try {
      return PersistenceMapper.versDomaine(
          jpa.saveAndFlush(PersistenceMapper.versEntite(affectation)));
    } catch (DataIntegrityViolationException e) {
      String cause = String.valueOf(e.getMostSpecificCause().getMessage());
      if (cause.contains(INDEX_AFFECTATION_ACTIVE)) {
        throw SuiviErreurs.conflitAffectation(String.valueOf(affectation.getMaterielId()));
      }
      throw e;
    }
  }

  @Override
  public Optional<Affectation> findById(UUID id) {
    return jpa.findById(id).map(PersistenceMapper::versDomaine);
  }

  @Override
  public List<Affectation> findActivesByMateriel(UUID materielId) {
    return jpa.findByMaterielIdAndStatutIn(materielId, StatutAffectation.ACTIFS).stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public List<Affectation> findRecentesByMateriel(UUID materielId, int limite) {
    return jpa.findByMaterielIdOrderByDateDebutDesc(materielId, PageRequest.of(0, limite)).stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public PageResponse<Affectation> rechercher(FiltreAffectations filtre, PageQuery page) {
    Specification<AffectationEntity> spec =
        Specs.tous(
            Specs.egal("materielId", filtre.materielId()),
            Specs.egal("statut", filtre.statut()),
            Specs.perimetre(CHANTIER, filtre.perimetre()));
    return Specs.page(
        jpa.findAll(spec, PageableFactory.toPageable(page)), PersistenceMapper::versDomaine);
  }

  @Override
  public List<Affectation> planning(FiltrePlanning filtre) {
    LocalDate du = filtre.periode().debut();
    LocalDate au = filtre.periode().fin();
    Specification<AffectationEntity> chevauche =
        (root, q, cb) ->
            cb.and(
                cb.lessThanOrEqualTo(root.get("dateDebut"), au),
                cb.greaterThanOrEqualTo(
                    cb.coalesce(root.<LocalDate>get("dateFinReelle"), root.get("dateFinPrevue")),
                    du));
    Specification<AffectationEntity> spec =
        Specs.tous(
            chevauche,
            Specs.perimetre(CHANTIER, filtre.perimetre()),
            categorie(filtre.categorie()));
    return jpa.findAll(spec, Sort.by("dateDebut", "id")).stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public List<Affectation> findActivesFinissantAuPlusTard(LocalDate limite) {
    return jpa
        .findByStatutInAndDateFinPrevueLessThanEqualAndAlerteFinEnvoyeeFalse(
            StatutAffectation.ACTIFS, limite)
        .stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public long compterActivesParChantier(UUID chantierId) {
    return jpa.countByChantierIdAndStatutIn(chantierId, StatutAffectation.ACTIFS);
  }

  private static Specification<AffectationEntity> categorie(CategorieMateriel categorie) {
    return (root, q, cb) -> {
      if (categorie == null) {
        return null;
      }
      Subquery<UUID> materiels = q.subquery(UUID.class);
      Root<MaterielEntity> m = materiels.from(MaterielEntity.class);
      materiels.select(m.get("id")).where(cb.equal(m.get("categorie"), categorie));
      return root.get("materielId").in(materiels);
    };
  }
}
