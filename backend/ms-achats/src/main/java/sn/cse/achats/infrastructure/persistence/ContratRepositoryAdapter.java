package sn.cse.achats.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.achats.infrastructure.persistence.entity.ContratAchatEntity;
import sn.cse.achats.infrastructure.persistence.mapper.ContratEntityMapper;
import sn.cse.achats.infrastructure.persistence.repository.ContratAchatJpaRepository;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Adaptateur JPA du port {@link ContratRepository}. */
@Repository
@Transactional
public class ContratRepositoryAdapter implements ContratRepository {

  private static final Map<String, String> TRI_ENGAGEMENTS =
      Map.of("dateDemande", "engagementDateDemande", "dateDecision", "engagementDateDecision");

  private final ContratAchatJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public ContratRepositoryAdapter(ContratAchatJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public ContratAchat enregistrer(ContratAchat contrat) {
    ContratAchatEntity entite = jpa.findById(contrat.getId()).orElseGet(ContratAchatEntity::new);
    ContratEntityMapper.copier(contrat, entite);
    return ContratEntityMapper.versDomaine(jpa.saveAndFlush(entite));
  }

  @Override
  public Optional<ContratAchat> parId(UUID id) {
    return jpa.findById(id).map(ContratEntityMapper::versDomaine);
  }

  @Override
  public Optional<ContratAchat> parAppelOffre(UUID appelOffreId) {
    return jpa.findByAppelOffreId(appelOffreId).map(ContratEntityMapper::versDomaine);
  }

  @Override
  public PageResponse<ContratAchat> rechercher(Filtre filtre, PageQuery page) {
    return Pages.reponse(
        jpa.findAll(specification(filtre), Pages.pageable(page)),
        page,
        ContratEntityMapper::versDomaine);
  }

  @Override
  public PageResponse<ContratAchat> avecEngagement(StatutApprobation statut, PageQuery page) {
    Specification<ContratAchatEntity> spec =
        (root, query, cb) ->
            statut == null
                ? cb.isNotNull(root.get("engagementStatut"))
                : cb.equal(root.get("engagementStatut"), statut);
    return Pages.reponse(
        jpa.findAll(spec, Pages.pageable(page, TRI_ENGAGEMENTS)),
        page,
        ContratEntityMapper::versDomaine);
  }

  @Override
  public List<ContratAchat> enExecutionEcheanceDepassee(LocalDate aujourdhui) {
    return jpa
        .findByStatutInAndDateLivraisonPrevueBefore(
            EnumSet.of(
                StatutContrat.SIGNE, StatutContrat.ENGAGEMENT_VALIDE, StatutContrat.EN_LIVRAISON),
            aujourdhui)
        .stream()
        .map(ContratEntityMapper::versDomaine)
        .toList();
  }

  @Override
  public List<ContratAchat> tous() {
    return jpa.findAll().stream().map(ContratEntityMapper::versDomaine).toList();
  }

  private static Specification<ContratAchatEntity> specification(Filtre filtre) {
    return (root, query, cb) -> {
      List<Predicate> predicats = new ArrayList<>();
      if (filtre.statut() != null) {
        predicats.add(cb.equal(root.get("statut"), filtre.statut()));
      }
      if (filtre.fournisseurId() != null) {
        predicats.add(cb.equal(root.get("fournisseurId"), filtre.fournisseurId()));
      }
      if (filtre.q() != null && !filtre.q().isBlank()) {
        String motif = "%" + filtre.q().trim().toLowerCase(Locale.ROOT) + "%";
        predicats.add(
            cb.or(
                cb.like(cb.lower(root.get("numero")), motif),
                cb.like(cb.lower(root.get("objet")), motif),
                cb.like(cb.lower(root.get("fournisseurNom")), motif)));
      }
      return cb.and(predicats.toArray(Predicate[]::new));
    };
  }
}
