package sn.cse.reporting.infrastructure.persistence;

import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import sn.cse.reporting.application.port.out.LectureReportingRepository;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/**
 * Lecture des vues pour un tableau de bord. Les tables volumineuses (affectations, maintenances,
 * pannes, besoins) sont filtrées par période et périmètre en SQL ; les référentiels (fournisseurs,
 * appels d'offres, contrats…) sont chargés en entier dans le périmètre, le domaine appliquant les
 * règles fines d'appartenance à la période.
 */
@Component
public class LectureReportingJdbcAdapter implements LectureReportingRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * @param jdbc accès JDBC à paramètres nommés
   */
  public LectureReportingJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public DonneesReporting charger(Periode periode, Perimetre perimetre) {
    if (perimetre.restreint() && perimetre.chantierIds().isEmpty()) {
      // Chef de chantier sans chantier attribué : seules les données transverses sont visibles.
      return new DonneesReporting(
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          tous("fournisseurs", VuesSql.FOURNISSEUR),
          tous("litiges", VuesSql.LITIGE));
    }
    Parametres p = new Parametres().date("du", periode.du()).date("au", periode.au());
    String filtre = "";
    if (perimetre.restreint()) {
      p.addValue("chantiers", List.copyOf(perimetre.chantierIds()), Types.OTHER);
      filtre = " AND chantier_id IN (:chantiers)";
    }
    String filtreId = perimetre.restreint() ? " WHERE id IN (:chantiers)" : "";
    return new DonneesReporting(
        jdbc.query("SELECT * FROM chantiers" + filtreId, p, VuesSql.CHANTIER),
        jdbc.query("SELECT * FROM materiels WHERE TRUE" + filtre, p, VuesSql.MATERIEL),
        jdbc.query(
            "SELECT * FROM affectations WHERE date_debut <= :au"
                + " AND (date_fin_reelle IS NULL OR date_fin_reelle >= :du)"
                + filtre,
            p,
            VuesSql.AFFECTATION),
        jdbc.query(
            "SELECT * FROM maintenances WHERE (statut = 'EN_COURS'"
                + " OR (coalesce(date_debut, date_planifiee) <= :au"
                + " AND (date_fin IS NULL OR date_fin >= :du)))",
            p,
            VuesSql.MAINTENANCE),
        jdbc.query(
            "SELECT * FROM pannes WHERE (resolue_le IS NULL OR signalee_le >= CAST(:du AS date))"
                + filtre,
            p,
            VuesSql.PANNE),
        jdbc.query(
            "SELECT * FROM besoins WHERE (date_soumission BETWEEN :du AND :au"
                + " OR statut = 'EN_ATTENTE')"
                + filtre,
            p,
            VuesSql.BESOIN),
        jdbc.query("SELECT * FROM appels_offres WHERE TRUE" + filtre, p, VuesSql.APPEL_OFFRE),
        jdbc.query("SELECT * FROM contrats WHERE TRUE" + filtre, p, VuesSql.CONTRAT),
        jdbc.query(
            perimetre.restreint()
                ? "SELECT f.* FROM factures f JOIN contrats c ON c.id = f.contrat_id"
                    + " WHERE c.chantier_id IN (:chantiers)"
                : "SELECT * FROM factures",
            p,
            VuesSql.FACTURE),
        tous("fournisseurs", VuesSql.FOURNISSEUR),
        tous("litiges", VuesSql.LITIGE));
  }

  @Override
  public List<ParcInstantane> instantanes(Periode periode) {
    return jdbc.query(
        "SELECT * FROM parc_instantanes WHERE jour <= :au AND jour >="
            + " (SELECT coalesce(max(jour), CAST(:du AS date)) FROM parc_instantanes WHERE jour <= :du)"
            + " ORDER BY jour",
        new Parametres().date("du", periode.du()).date("au", periode.au()),
        VuesSql.INSTANTANE);
  }

  private <T> List<T> tous(String table, RowMapper<T> mapper) {
    return jdbc.query("SELECT * FROM " + table, mapper);
  }
}
