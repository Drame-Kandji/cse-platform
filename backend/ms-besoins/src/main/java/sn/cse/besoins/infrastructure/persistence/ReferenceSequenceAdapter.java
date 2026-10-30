package sn.cse.besoins.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import sn.cse.besoins.application.port.out.GenerateurReference;
import sn.cse.besoins.infrastructure.config.BesoinsProperties;

/**
 * Références {@code BES-<année>-<n°>} tirées d'une séquence PostgreSQL par année (créée à la
 * demande ; les années courantes sont pré-créées par la migration). Les séquences garantissent
 * l'unicité sans verrou applicatif.
 */
@Component
class ReferenceSequenceAdapter implements GenerateurReference {

  private static final int ANNEE_MIN = 2000;
  private static final int ANNEE_MAX = 9999;

  private final JdbcTemplate jdbc;
  private final BesoinsProperties.Reference format;

  ReferenceSequenceAdapter(JdbcTemplate jdbc, BesoinsProperties properties) {
    this.jdbc = jdbc;
    this.format = properties.reference();
  }

  @Override
  public String prochaineReference(int annee) {
    // L'année est un entier borné : le nom de séquence ne peut pas être injecté.
    if (annee < ANNEE_MIN || annee > ANNEE_MAX) {
      throw new IllegalArgumentException("Année de référence invalide : " + annee);
    }
    String sequence = "seq_reference_besoin_" + annee;
    jdbc.execute("CREATE SEQUENCE IF NOT EXISTS " + sequence);
    Long numero = jdbc.queryForObject("SELECT nextval('" + sequence + "')", Long.class);
    return String.format("%s-%d-%0" + format.chiffres() + "d", format.prefixe(), annee, numero);
  }
}
