package sn.cse.achats.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.achats.infrastructure.persistence.entity.AppelOffreEntity;
import sn.cse.achats.infrastructure.persistence.mapper.AppelOffreEntityMapper;
import sn.cse.achats.infrastructure.persistence.repository.AppelOffreJpaRepository;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Adaptateur JPA du port {@link AppelOffreRepository}. */
@Repository
@Transactional
public class AppelOffreRepositoryAdapter implements AppelOffreRepository {

  private final AppelOffreJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public AppelOffreRepositoryAdapter(AppelOffreJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public AppelOffre enregistrer(AppelOffre appelOffre) {
    AppelOffreEntity entite = jpa.findById(appelOffre.getId()).orElseGet(AppelOffreEntity::new);
    AppelOffreEntityMapper.copier(appelOffre, entite);
    return AppelOffreEntityMapper.versDomaine(jpa.saveAndFlush(entite));
  }

  @Override
  public Optional<AppelOffre> parId(UUID id) {
    return jpa.findById(id).map(AppelOffreEntityMapper::versDomaine);
  }

  @Override
  public PageResponse<AppelOffre> rechercher(Filtre filtre, PageQuery page) {
    return Pages.reponse(
        jpa.findAll(specification(filtre), Pages.pageable(page)),
        page,
        AppelOffreEntityMapper::versDomaine);
  }

  @Override
  public List<UUID> idsPubliesEchus(Instant maintenant) {
    return jpa.idsEchus(StatutAO.PUBLIE, maintenant);
  }

  @Override
  public PageResponse<AppelOffre> ouvertsAuFournisseur(
      UUID fournisseurId, Instant maintenant, PageQuery page) {
    return Pages.reponse(
        jpa.ouvertsAuFournisseur(
            StatutAO.PUBLIE,
            maintenant,
            TypeAppelOffre.APPEL_OFFRES,
            fournisseurId,
            Pages.pageable(page)),
        page,
        AppelOffreEntityMapper::versDomaine);
  }

  @Override
  public PageResponse<AppelOffre> avecOffreDu(UUID fournisseurId, PageQuery page) {
    return Pages.reponse(
        jpa.avecOffreDu(fournisseurId, Pages.pageable(page)),
        page,
        AppelOffreEntityMapper::versDomaine);
  }

  @Override
  public long compter() {
    return jpa.count();
  }

  private static Specification<AppelOffreEntity> specification(Filtre filtre) {
    return (root, query, cb) -> {
      List<Predicate> predicats = new ArrayList<>();
      if (filtre.statut() != null) {
        predicats.add(cb.equal(root.get("statut"), filtre.statut()));
      }
      if (filtre.type() != null) {
        predicats.add(cb.equal(root.get("type"), filtre.type()));
      }
      if (filtre.q() != null && !filtre.q().isBlank()) {
        String motif = "%" + filtre.q().trim().toLowerCase(Locale.ROOT) + "%";
        predicats.add(
            cb.or(
                cb.like(cb.lower(root.get("reference")), motif),
                cb.like(cb.lower(root.get("objet")), motif)));
      }
      return cb.and(predicats.toArray(Predicate[]::new));
    };
  }
}
