package sn.cse.fournisseurs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.infrastructure.seed.DevDataSeeder;

/** Jeu de démonstration (profil dev) : 5 fournisseurs actifs sans historique, idempotence. */
@SpringBootTest
@Import(PostgresTestConfiguration.class)
@ActiveProfiles({"test", "dev"})
class DevDataSeederIntegrationTest {

  @Autowired private FournisseurRepository fournisseurs;
  @Autowired private DevDataSeeder seeder;
  @Autowired private JdbcTemplate jdbc;

  private static UUID f(int n) {
    return UUID.fromString(String.format("f0000000-0000-0000-0000-%012d", n));
  }

  @Test
  void should_semer_les_cinq_fournisseurs_actifs_du_contrat() {
    for (int n = 1; n <= 5; n++) {
      Fournisseur fournisseur = fournisseurs.parId(f(n)).orElseThrow();
      assertThat(fournisseur.getStatut()).isEqualTo(StatutFournisseur.ACTIF);
      assertThat(fournisseur.nombreDocumentsExpires(LocalDate.now())).isZero();
    }
    assertThat(fournisseurs.parId(f(1)).orElseThrow().getProfil().raisonSociale())
        .isEqualTo("Sahel Engins SA");
    assertThat(fournisseurs.parId(f(6))).isEmpty();
    for (String table : List.of("evaluations", "litiges")) {
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM " + table + " WHERE fournisseur_id::text LIKE 'f0000000-%'",
                  Integer.class))
          .isZero();
    }
  }

  @Test
  void should_etre_idempotent() throws Exception {
    Integer avant = jdbc.queryForObject("SELECT count(*) FROM fournisseurs", Integer.class);
    seeder.run(new DefaultApplicationArguments());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM fournisseurs", Integer.class))
        .isEqualTo(avant);
  }
}
