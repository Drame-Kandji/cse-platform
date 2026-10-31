package sn.cse.audit.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.audit.application.port.out.JournalAuditRepository;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.audit.domain.model.StatistiquesAudit.TotalParService;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/**
 * Adaptateur JPA du journal d'audit. Ajout par {@code persist} (aucune lecture préalable), lecture
 * filtrée par l'API Criteria (paramètres typés, pas de concaténation SQL).
 */
@Component
public class JournalAuditPersistenceAdapter implements JournalAuditRepository {

  private final AuditLogJpaRepository repository;

  @PersistenceContext private EntityManager em;

  /**
   * @param repository accès Spring Data
   */
  public JournalAuditPersistenceAdapter(AuditLogJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean existePourEvenement(UUID eventId) {
    return repository.existsByEventId(eventId);
  }

  @Override
  public void ajouter(AuditLog entree) {
    em.persist(AuditLogEntity.depuis(entree));
  }

  @Override
  public PageResponse<AuditLog> rechercher(CritereRechercheAudit critere, PageQuery page) {
    CriteriaBuilder cb = em.getCriteriaBuilder();

    CriteriaQuery<AuditLogEntity> requete = cb.createQuery(AuditLogEntity.class);
    Root<AuditLogEntity> racine = requete.from(AuditLogEntity.class);
    requete.where(predicats(cb, racine, critere));
    requete.orderBy(ordre(cb, racine, page));
    List<AuditLog> contenu =
        em
            .createQuery(requete)
            .setFirstResult(page.page() * page.size())
            .setMaxResults(page.size())
            .getResultList()
            .stream()
            .map(AuditLogEntity::versDomaine)
            .toList();

    CriteriaQuery<Long> comptage = cb.createQuery(Long.class);
    Root<AuditLogEntity> racineComptage = comptage.from(AuditLogEntity.class);
    comptage.select(cb.count(racineComptage)).where(predicats(cb, racineComptage, critere));
    long total = em.createQuery(comptage).getSingleResult();

    return PageResponse.of(contenu, page.page(), page.size(), total);
  }

  @Override
  public long compterTout() {
    return repository.count();
  }

  @Override
  public long compterDepuis(Instant depuis) {
    return repository.countByOccurredAtGreaterThanEqual(depuis);
  }

  @Override
  public List<TotalParService> totalParService() {
    return repository.totalParService().stream()
        .map(v -> new TotalParService(v.getService(), v.getTotal()))
        .toList();
  }

  private static Predicate[] predicats(
      CriteriaBuilder cb, Root<AuditLogEntity> r, CritereRechercheAudit c) {
    List<Predicate> p = new ArrayList<>();
    if (c.utilisateurId() != null) {
      p.add(cb.equal(r.get("utilisateurId"), c.utilisateurId()));
    }
    if (c.entite() != null) {
      p.add(cb.equal(r.get("entite"), c.entite()));
    }
    if (c.action() != null) {
      p.add(cb.equal(r.get("action"), c.action()));
    }
    if (c.service() != null) {
      p.add(cb.equal(r.get("service"), c.service()));
    }
    c.debut().ifPresent(d -> p.add(cb.greaterThanOrEqualTo(r.get("occurredAt"), d)));
    c.fin().ifPresent(f -> p.add(cb.lessThan(r.get("occurredAt"), f)));
    if (c.q() != null) {
      String motif = "%" + echapper(c.q().toLowerCase(Locale.ROOT)) + "%";
      p.add(
          cb.or(
              cb.like(cb.lower(r.get("resume")), motif, '\\'),
              cb.like(cb.lower(r.get("entiteId")), motif, '\\'),
              cb.like(cb.lower(r.get("utilisateurNom")), motif, '\\'),
              cb.like(cb.lower(r.get("action")), motif, '\\'),
              cb.like(cb.lower(r.get("entite")), motif, '\\')));
    }
    return p.toArray(Predicate[]::new);
  }

  private static List<Order> ordre(CriteriaBuilder cb, Root<AuditLogEntity> r, PageQuery page) {
    List<Order> ordres = new ArrayList<>();
    if (page.sortField() != null) {
      ordres.add(
          page.ascending() ? cb.asc(r.get(page.sortField())) : cb.desc(r.get(page.sortField())));
    }
    // Tri secondaire stable pour une pagination déterministe.
    ordres.add(cb.desc(r.get("id")));
    return ordres;
  }

  // Les caractères génériques saisis par l'utilisateur sont recherchés littéralement.
  private static String echapper(String texte) {
    return texte.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
