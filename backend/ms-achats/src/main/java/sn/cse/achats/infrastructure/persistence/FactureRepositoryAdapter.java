package sn.cse.achats.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.achats.infrastructure.persistence.entity.FactureEntity;
import sn.cse.achats.infrastructure.persistence.mapper.FactureEntityMapper;
import sn.cse.achats.infrastructure.persistence.repository.FactureJpaRepository;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Adaptateur JPA du port {@link FactureRepository}. */
@Repository
@Transactional
public class FactureRepositoryAdapter implements FactureRepository {

  private static final EnumSet<StatutFacture> OUVERTES =
      EnumSet.of(StatutFacture.RECUE, StatutFacture.MANDATEE);

  private final FactureJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public FactureRepositoryAdapter(FactureJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Facture enregistrer(Facture facture) {
    FactureEntity entite = jpa.findById(facture.getId()).orElseGet(FactureEntity::new);
    FactureEntityMapper.copier(facture, entite);
    return FactureEntityMapper.versDomaine(jpa.saveAndFlush(entite));
  }

  @Override
  public Optional<Facture> parId(UUID id) {
    return jpa.findById(id).map(FactureEntityMapper::versDomaine);
  }

  @Override
  public PageResponse<Facture> rechercher(Filtre filtre, LocalDate aujourdhui, PageQuery page) {
    return Pages.reponse(
        jpa.findAll(specification(filtre, aujourdhui), Pages.pageable(page)),
        page,
        FactureEntityMapper::versDomaine);
  }

  @Override
  public List<Facture> parContrat(UUID contratId) {
    return jpa.findByContratId(contratId).stream().map(FactureEntityMapper::versDomaine).toList();
  }

  @Override
  public boolean existe(UUID fournisseurId, String numero) {
    return jpa.existsByFournisseurIdAndNumero(fournisseurId, numero);
  }

  @Override
  public List<Facture> toutes() {
    return jpa.findAll().stream().map(FactureEntityMapper::versDomaine).toList();
  }

  private static Specification<FactureEntity> specification(Filtre filtre, LocalDate aujourdhui) {
    return (root, query, cb) -> {
      List<Predicate> predicats = new ArrayList<>();
      if (filtre.statut() != null) {
        predicats.add(cb.equal(root.get("statut"), filtre.statut()));
      }
      if (filtre.fournisseurId() != null) {
        predicats.add(cb.equal(root.get("fournisseurId"), filtre.fournisseurId()));
      }
      if (filtre.contratId() != null) {
        predicats.add(cb.equal(root.get("contratId"), filtre.contratId()));
      }
      if (filtre.enRetard() != null) {
        // Même définition que Facture.estEnRetard : non payée et échéance dépassée.
        Predicate retard =
            cb.and(
                root.get("statut").in(OUVERTES), cb.lessThan(root.get("dateEcheance"), aujourdhui));
        predicats.add(filtre.enRetard() ? retard : cb.not(retard));
      }
      return cb.and(predicats.toArray(Predicate[]::new));
    };
  }
}
