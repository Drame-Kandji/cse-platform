package sn.cse.achats.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.out.ReferenceGenerator;

/**
 * Références lisibles issues d'une séquence PostgreSQL par année ({@code seq_ref_ao_2026}, {@code
 * seq_ref_contrat_2026}), créée à la première utilisation. Les séquences sont non transactionnelles
 * : deux transactions concurrentes n'obtiennent jamais le même numéro.
 */
@Component
public class SequenceReferenceGenerator implements ReferenceGenerator {

  private static final String FORMAT = "%s-%d-%03d";

  private final JdbcTemplate jdbc;

  /**
   * @param jdbc accès JDBC
   */
  public SequenceReferenceGenerator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public String prochaineReferenceAppelOffre(int annee) {
    return String.format(FORMAT, "AO", annee, suivant("seq_ref_ao_" + annee));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public String prochainNumeroContrat(int annee) {
    return String.format(FORMAT, "CTR", annee, suivant("seq_ref_contrat_" + annee));
  }

  // Le nom est construit à partir d'un préfixe constant et d'un entier : aucune injection possible.
  private long suivant(String sequence) {
    // Verrou transactionnel : sérialise la création concurrente de la séquence de l'année.
    jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", sequence);
    jdbc.execute("CREATE SEQUENCE IF NOT EXISTS " + sequence + " START WITH 1 INCREMENT BY 1");
    Long valeur = jdbc.queryForObject("SELECT nextval(?::regclass)", Long.class, sequence);
    if (valeur == null) {
      throw new IllegalStateException("Séquence " + sequence + " indisponible");
    }
    return valeur;
  }
}
