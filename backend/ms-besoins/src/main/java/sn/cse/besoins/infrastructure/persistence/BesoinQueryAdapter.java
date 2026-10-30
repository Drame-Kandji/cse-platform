package sn.cse.besoins.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.cse.besoins.application.port.out.BesoinQueryPort;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.application.query.CriteresRechercheBesoins;
import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.service.BesoinAConsolider;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;

/** Lectures des besoins : recherche paginée (JPA Criteria), consolidation et agrégats SQL. */
@Repository
class BesoinQueryAdapter implements BesoinQueryPort {

  private static final char ECHAPPEMENT = '\\';

  private final BesoinMaterielJpaRepository jpa;
  private final NamedParameterJdbcTemplate jdbc;

  BesoinQueryAdapter(BesoinMaterielJpaRepository jpa, NamedParameterJdbcTemplate jdbc) {
    this.jpa = jpa;
    this.jdbc = jdbc;
  }

  @Override
  public PageResponse<BesoinVue> rechercher(CriteresRechercheBesoins criteres, PageQuery page) {
    if (criteres.chantiersAutorises() != null && criteres.chantiersAutorises().isEmpty()) {
      return PageResponse.of(List.of(), page.page(), page.size(), 0);
    }
    Page<BesoinMaterielEntity> resultat =
        jpa.findAll(specification(criteres), PageableFactory.toPageable(page));
    return PageResponse.of(
        resultat.getContent().stream().map(BesoinEntityMapper::versVue).toList(),
        page.page(),
        page.size(),
        resultat.getTotalElements());
  }

  @Override
  public List<BesoinAConsolider> aConsolider(Set<StatutBesoin> statuts, int limite) {
    Specification<BesoinMaterielEntity> spec = (root, q, cb) -> root.get("statut").in(statuts);
    return jpa.findAll(spec, PageRequest.of(0, limite, Sort.by("createdAt"))).stream()
        .map(
            e ->
                new BesoinAConsolider(
                    e.id,
                    e.chantierId,
                    e.chantierNom,
                    e.categorie,
                    e.designation,
                    e.quantite,
                    e.priorite,
                    e.dateBesoin))
        .toList();
  }

  @Override
  public StatistiquesBesoins statistiques(Set<UUID> chantiers) {
    if (chantiers != null && chantiers.isEmpty()) {
      return new StatistiquesBesoins(0, Map.of(), Map.of(), null);
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    String filtre = "";
    if (chantiers != null) {
      filtre = " WHERE chantier_id IN (:chantiers)";
      params.addValue("chantiers", chantiers);
    }
    Map<StatutBesoin, Long> parStatut =
        compter("statut", filtre, params, StatutBesoin.class, StatutBesoin::valueOf);
    Map<Priorite, Long> parPriorite =
        compter("priorite", filtre, params, Priorite.class, Priorite::valueOf);
    String conditionDelai =
        (filtre.isEmpty() ? " WHERE " : filtre + " AND ") + "date_validation IS NOT NULL";
    Double delai =
        jdbc.queryForObject(
            "SELECT avg(extract(epoch FROM (date_validation - created_at))) / 3600.0"
                + " FROM besoins_materiels"
                + conditionDelai,
            params,
            Double.class);
    long total = parStatut.values().stream().mapToLong(Long::longValue).sum();
    return new StatistiquesBesoins(
        total, parStatut, parPriorite, delai == null ? null : Math.round(delai * 10) / 10.0);
  }

  private <E extends Enum<E>> Map<E, Long> compter(
      String colonne,
      String filtre,
      MapSqlParameterSource params,
      Class<E> type,
      Function<String, E> conversion) {
    Map<E, Long> resultat = new EnumMap<>(type);
    jdbc.query(
        "SELECT "
            + colonne
            + "::text AS cle, count(*) AS nombre FROM besoins_materiels"
            + filtre
            + " GROUP BY "
            + colonne,
        params,
        rs -> {
          resultat.put(conversion.apply(rs.getString("cle")), rs.getLong("nombre"));
        });
    return resultat;
  }

  private static Specification<BesoinMaterielEntity> specification(CriteresRechercheBesoins c) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      if (c.chantiersAutorises() != null) {
        p.add(root.get("chantierId").in(c.chantiersAutorises()));
      }
      if (c.chantierId() != null) {
        p.add(cb.equal(root.get("chantierId"), c.chantierId()));
      }
      if (c.statut() != null) {
        p.add(cb.equal(root.get("statut"), c.statut()));
      }
      if (c.priorite() != null) {
        p.add(cb.equal(root.get("priorite"), c.priorite()));
      }
      if (c.categorie() != null) {
        p.add(cb.equal(root.get("categorie"), c.categorie()));
      }
      if (c.du() != null) {
        p.add(
            cb.greaterThanOrEqualTo(
                root.get("createdAt"), c.du().atStartOfDay(ZoneOffset.UTC).toInstant()));
      }
      if (c.au() != null) {
        p.add(
            cb.lessThan(
                root.get("createdAt"),
                c.au().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
      }
      if (c.q() != null) {
        String motif = "%" + echapper(c.q().toLowerCase(Locale.ROOT)) + "%";
        p.add(
            cb.or(
                cb.like(cb.lower(root.get("reference")), motif, ECHAPPEMENT),
                cb.like(cb.lower(root.get("designation")), motif, ECHAPPEMENT),
                cb.like(cb.lower(root.get("specifications")), motif, ECHAPPEMENT)));
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static String echapper(String texte) {
    return texte.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
