package sn.cse.fournisseurs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.fournisseurs.application.port.out.CatalogueRepository;
import sn.cse.fournisseurs.application.port.out.EvaluationRepository;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.service.ScorePolicy;
import sn.cse.fournisseurs.infrastructure.seed.DevDataSeeder;

/** Jeu de démonstration (profil dev) : contenu conforme au contrat §12 et idempotence. */
@SpringBootTest
@Import(PostgresTestConfiguration.class)
@ActiveProfiles({"test", "dev"})
class DevDataSeederIntegrationTest {

  @Autowired private FournisseurRepository fournisseurs;
  @Autowired private EvaluationRepository evaluations;
  @Autowired private CatalogueRepository catalogue;
  @Autowired private DevDataSeeder seeder;
  @Autowired private ScorePolicy politique;
  @Autowired private JdbcTemplate jdbc;

  private static UUID f(int n) {
    return UUID.fromString(String.format("f0000000-0000-0000-0000-%012d", n));
  }

  @Test
  void should_semer_les_sept_fournisseurs_du_contrat() {
    for (int n = 1; n <= 5; n++) {
      assertThat(fournisseurs.parId(f(n)))
          .get()
          .extracting(Fournisseur::getStatut)
          .isEqualTo(StatutFournisseur.ACTIF);
    }
    Fournisseur f6 = fournisseurs.parId(f(6)).orElseThrow();
    assertThat(f6.getProfil().raisonSociale()).isEqualTo("Ndiambour Matériaux");
    assertThat(f6.getStatut()).isEqualTo(StatutFournisseur.SUSPENDU);
    assertThat(f6.getDemandeBlacklistage())
        .get()
        .satisfies(d -> assertThat(d.demandeurNom()).isEqualTo("Fatou Sarr"));
    assertThat(politique.estSousSeuilAlerte(f6.getPerformance().scorePerformance())).isTrue();
    assertThat(fournisseurs.parId(f(7)))
        .get()
        .extracting(Fournisseur::getStatut)
        .isEqualTo(StatutFournisseur.EN_ATTENTE_VALIDATION);

    Fournisseur f1 = fournisseurs.parId(f(1)).orElseThrow();
    assertThat(f1.getProfil().raisonSociale()).isEqualTo("Sahel Engins SA");
    assertThat(f1.getPerformance().scorePerformance())
        .isGreaterThan(f6.getPerformance().scorePerformance());
    assertThat(f1.getPerformance().tauxLivraisonALHeure()).isPresent();
    assertThat(evaluations.parFournisseur(f(1))).hasSize(5);
    assertThat(evaluations.parFournisseur(f(1)).get(4).date())
        .isBefore(java.time.Instant.now().minus(java.time.Duration.ofDays(240)));
    assertThat(fournisseurs.parId(f(3)).orElseThrow().nombreDocumentsExpires(LocalDate.now()))
        .isEqualTo(1);
    assertThat(catalogue.historique(f(5), "ATC-CEM425")).hasSize(3);

    Integer litiges =
        jdbc.queryForObject(
            "SELECT count(*) FROM litiges WHERE fournisseur_id IN (?,?,?,?,?,?,?)",
            Integer.class,
            f(1),
            f(2),
            f(3),
            f(4),
            f(5),
            f(6),
            f(7));
    assertThat(litiges).isEqualTo(5);
    Integer statutsLitiges =
        jdbc.queryForObject(
            "SELECT count(DISTINCT statut) FROM litiges WHERE fournisseur_id IN (?,?,?,?,?,?,?)",
            Integer.class,
            f(1),
            f(2),
            f(3),
            f(4),
            f(5),
            f(6),
            f(7));
    assertThat(statutsLitiges).isEqualTo(4);
    Integer documentsExpires =
        jdbc.queryForObject(
            "SELECT count(*) FROM outbox_event WHERE event_type = 'DOCUMENT_EXPIRE'",
            Integer.class);
    assertThat(documentsExpires).isGreaterThanOrEqualTo(4);
  }

  @Test
  void should_etre_idempotent() throws Exception {
    Integer avant = jdbc.queryForObject("SELECT count(*) FROM evaluations", Integer.class);
    seeder.run(new DefaultApplicationArguments());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM evaluations", Integer.class))
        .isEqualTo(avant);
  }
}
