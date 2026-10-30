package sn.cse.fournisseurs.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import sn.cse.fournisseurs.application.port.out.GenerateurReference;

/**
 * Références lisibles {@code LIT-AAAA-NNN} tirées d'une séquence PostgreSQL par année ({@code
 * seq_reference_litige_AAAA}), créée à la première utilisation de l'année : la numérotation repart
 * à 1 chaque année sans risque de doublon en concurrence.
 */
@Component
public class SequenceReferenceGenerateur implements GenerateurReference {

  private static final String PREFIXE_LITIGE = "LIT";
  private static final int ANNEE_MIN = 2000;
  private static final int ANNEE_MAX = 9999;

  private final JdbcTemplate jdbc;

  /**
   * @param jdbc accès JDBC
   */
  public SequenceReferenceGenerateur(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String prochaineReferenceLitige(int annee) {
    if (annee < ANNEE_MIN || annee > ANNEE_MAX) {
      throw new IllegalArgumentException("Année de référence invalide : " + annee);
    }
    // Le nom de séquence ne contient qu'un entier validé : aucune injection possible.
    String sequence = "seq_reference_litige_" + annee;
    jdbc.execute("CREATE SEQUENCE IF NOT EXISTS " + sequence);
    Long numero = jdbc.queryForObject("SELECT nextval('" + sequence + "')", Long.class);
    return String.format("%s-%d-%03d", PREFIXE_LITIGE, annee, numero);
  }
}
