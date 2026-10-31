package sn.cse.reporting.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/**
 * Écriture des vues en JDBC. Les vues sont des projections reconstruites à partir d'événements : un
 * upsert {@code INSERT … ON CONFLICT DO UPDATE} exprime directement la sémantique « instantané par
 * agrégat » et règle la course entre deux topics créant la même référence.
 */
@Component
public class ProjectionJdbcAdapter implements ProjectionRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * @param jdbc accès JDBC à paramètres nommés
   */
  public ProjectionJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ChantierVue> chantier(UUID id) {
    return parId("chantiers", id, VuesSql.CHANTIER);
  }

  @Override
  public void enregistrer(ChantierVue vue) {
    jdbc.update(VuesSql.UPSERT_CHANTIER, VuesSql.params(vue));
  }

  @Override
  public void referencer(ChantierVue reference) {
    jdbc.update(VuesSql.REFERENCE_CHANTIER, VuesSql.params(reference));
  }

  @Override
  public Optional<MaterielVue> materiel(UUID id) {
    return parId("materiels", id, VuesSql.MATERIEL);
  }

  @Override
  public void enregistrer(MaterielVue vue) {
    jdbc.update(VuesSql.UPSERT_MATERIEL, VuesSql.params(vue));
  }

  @Override
  public List<MaterielVue> materiels() {
    return jdbc.query("SELECT * FROM materiels", VuesSql.MATERIEL);
  }

  @Override
  public Optional<AffectationVue> affectation(UUID id) {
    return parId("affectations", id, VuesSql.AFFECTATION);
  }

  @Override
  public void enregistrer(AffectationVue vue) {
    jdbc.update(VuesSql.UPSERT_AFFECTATION, VuesSql.params(vue));
  }

  @Override
  public Optional<MaintenanceVue> maintenance(UUID id) {
    return parId("maintenances", id, VuesSql.MAINTENANCE);
  }

  @Override
  public void enregistrer(MaintenanceVue vue) {
    jdbc.update(VuesSql.UPSERT_MAINTENANCE, VuesSql.params(vue));
  }

  @Override
  public Optional<Instant> derniereClotureMaintenance(UUID materielId) {
    OffsetDateTime max =
        jdbc.queryForObject(
            "SELECT max(cloturee_le) FROM maintenances WHERE materiel_id = :m",
            new Parametres().uuid("m", materielId),
            OffsetDateTime.class);
    return Optional.ofNullable(max).map(OffsetDateTime::toInstant);
  }

  @Override
  public Optional<PanneVue> panne(UUID id) {
    return parId("pannes", id, VuesSql.PANNE);
  }

  @Override
  public void enregistrer(PanneVue vue) {
    jdbc.update(VuesSql.UPSERT_PANNE, VuesSql.params(vue));
  }

  @Override
  public List<PanneVue> pannesOuvertes(UUID materielId) {
    return jdbc.query(
        "SELECT * FROM pannes WHERE materiel_id = :m AND resolue_le IS NULL",
        new Parametres().uuid("m", materielId),
        VuesSql.PANNE);
  }

  @Override
  public Optional<BesoinVue> besoin(UUID id) {
    return parId("besoins", id, VuesSql.BESOIN);
  }

  @Override
  public void enregistrer(BesoinVue vue) {
    jdbc.update(VuesSql.UPSERT_BESOIN, VuesSql.params(vue));
  }

  @Override
  public Optional<AppelOffreVue> appelOffre(UUID id) {
    return parId("appels_offres", id, VuesSql.APPEL_OFFRE);
  }

  @Override
  public void enregistrer(AppelOffreVue vue) {
    jdbc.update(VuesSql.UPSERT_APPEL_OFFRE, VuesSql.params(vue));
  }

  @Override
  public Optional<ContratVue> contrat(UUID id) {
    return parId("contrats", id, VuesSql.CONTRAT);
  }

  @Override
  public void enregistrer(ContratVue vue) {
    jdbc.update(VuesSql.UPSERT_CONTRAT, VuesSql.params(vue));
  }

  @Override
  public Optional<FactureVue> facture(UUID id) {
    return parId("factures", id, VuesSql.FACTURE);
  }

  @Override
  public void enregistrer(FactureVue vue) {
    jdbc.update(VuesSql.UPSERT_FACTURE, VuesSql.params(vue));
  }

  @Override
  public Optional<FournisseurVue> fournisseur(UUID id) {
    return parId("fournisseurs", id, VuesSql.FOURNISSEUR);
  }

  @Override
  public void enregistrer(FournisseurVue vue) {
    jdbc.update(VuesSql.UPSERT_FOURNISSEUR, VuesSql.params(vue));
  }

  @Override
  public void referencer(FournisseurVue reference) {
    jdbc.update(VuesSql.REFERENCE_FOURNISSEUR, VuesSql.params(reference));
  }

  @Override
  public Optional<LitigeVue> litige(UUID id) {
    return parId("litiges", id, VuesSql.LITIGE);
  }

  @Override
  public void enregistrer(LitigeVue vue) {
    jdbc.update(VuesSql.UPSERT_LITIGE, VuesSql.params(vue));
  }

  @Override
  public void remplacerInstantanes(LocalDate jour, List<ParcInstantane> lignes) {
    jdbc.update(
        "DELETE FROM parc_instantanes WHERE jour = :jour", new Parametres().date("jour", jour));
    if (!lignes.isEmpty()) {
      jdbc.batchUpdate(
          VuesSql.INSERT_INSTANTANE,
          lignes.stream().map(VuesSql::params).toArray(SqlParameterSource[]::new));
    }
  }

  // Le nom de table provient exclusivement des constantes de cette classe (jamais d'une saisie).
  private <T> Optional<T> parId(String table, UUID id, RowMapper<T> mapper) {
    return jdbc
        .query(
            "SELECT * FROM " + table + " WHERE id = :id", new Parametres().uuid("id", id), mapper)
        .stream()
        .findFirst();
  }
}
