package sn.cse.fournisseurs.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.application.query.CompteurLitiges;
import sn.cse.fournisseurs.domain.model.ActionLitige;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.StatutLitige;
import sn.cse.fournisseurs.infrastructure.persistence.entity.ActionLitigeEmbeddable;
import sn.cse.fournisseurs.infrastructure.persistence.entity.LitigeEntity;
import sn.cse.fournisseurs.infrastructure.persistence.repository.LitigeJpaRepository;

/** Adaptateur JPA du port {@link LitigeRepository}. */
@Repository
@Transactional
public class LitigeRepositoryAdapter implements LitigeRepository {

  private final LitigeJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public LitigeRepositoryAdapter(LitigeJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Litige sauvegarder(Litige litige) {
    LitigeEntity e =
        jpa.findById(litige.getId())
            .orElseGet(
                () -> {
                  LitigeEntity nouvelle = new LitigeEntity();
                  nouvelle.setId(litige.getId());
                  nouvelle.setReference(litige.getReference());
                  nouvelle.setFournisseurId(litige.getFournisseurId());
                  return nouvelle;
                });
    e.setRaisonSociale(litige.getRaisonSociale());
    e.setContratId(litige.getContratId().orElse(null));
    e.setObjet(litige.getObjet());
    e.setDescription(litige.getDescription());
    e.setGravite(litige.getGravite());
    e.setStatut(litige.getStatut());
    e.setDateOuverture(litige.getDateOuverture());
    e.setDateResolution(litige.getDateResolution().orElse(null));
    // La chronologie est en ajout seul : seules les nouvelles entrées sont ajoutées.
    List<ActionLitige> actions = litige.getActions();
    for (int i = e.getActions().size(); i < actions.size(); i++) {
      ActionLitige a = actions.get(i);
      e.getActions()
          .add(
              new ActionLitigeEmbeddable(
                  a.date(), a.auteurId(), a.auteurNom(), a.type(), a.commentaire()));
    }
    return versDomaine(jpa.saveAndFlush(e));
  }

  @Override
  public Optional<Litige> parId(UUID id) {
    return jpa.findById(id).map(LitigeRepositoryAdapter::versDomaine);
  }

  @Override
  public PageResponse<Litige> rechercher(StatutLitige statut, UUID fournisseurId, PageQuery page) {
    Specification<LitigeEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicats = new ArrayList<>();
          if (statut != null) {
            predicats.add(cb.equal(root.get("statut"), statut));
          }
          if (fournisseurId != null) {
            predicats.add(cb.equal(root.get("fournisseurId"), fournisseurId));
          }
          return cb.and(predicats.toArray(Predicate[]::new));
        };
    return Pages.versReponse(
        jpa.findAll(spec, PageableFactory.toPageable(page)), LitigeRepositoryAdapter::versDomaine);
  }

  @Override
  public Map<UUID, CompteurLitiges> compterParFournisseur(Collection<UUID> fournisseurIds) {
    Map<UUID, CompteurLitiges> resultat = new HashMap<>();
    if (fournisseurIds.isEmpty()) {
      return resultat;
    }
    for (Object[] ligne : jpa.compterParFournisseurEtStatut(fournisseurIds)) {
      UUID id = (UUID) ligne[0];
      StatutLitige statut = (StatutLitige) ligne[1];
      int nombre = ((Number) ligne[2]).intValue();
      CompteurLitiges c = resultat.getOrDefault(id, CompteurLitiges.AUCUN);
      resultat.put(
          id,
          switch (statut) {
            case OUVERT, EN_COURS ->
                new CompteurLitiges(c.ouverts() + nombre, c.resolus(), c.classes());
            case RESOLU -> new CompteurLitiges(c.ouverts(), c.resolus() + nombre, c.classes());
            case CLASSE -> new CompteurLitiges(c.ouverts(), c.resolus(), c.classes() + nombre);
          });
    }
    return resultat;
  }

  @Override
  public long compterOuverts() {
    return jpa.countByStatutIn(EnumSet.of(StatutLitige.OUVERT, StatutLitige.EN_COURS));
  }

  private static Litige versDomaine(LitigeEntity e) {
    return Litige.reconstituer(
        new Litige.EtatLitige(
            e.getId(),
            e.getReference(),
            e.getFournisseurId(),
            e.getRaisonSociale(),
            e.getContratId(),
            e.getObjet(),
            e.getDescription(),
            e.getGravite(),
            e.getStatut(),
            e.getDateOuverture(),
            e.getDateResolution(),
            e.getActions().stream()
                .map(
                    a ->
                        new ActionLitige(
                            a.getDate(),
                            a.getAuteurId(),
                            a.getAuteurNom(),
                            a.getType(),
                            a.getCommentaire()))
                .toList(),
            e.getVersion() == null ? 0L : e.getVersion()));
  }
}
