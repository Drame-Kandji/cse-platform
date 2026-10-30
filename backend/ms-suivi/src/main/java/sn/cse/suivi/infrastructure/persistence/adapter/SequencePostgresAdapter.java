package sn.cse.suivi.infrastructure.persistence.adapter;

import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.suivi.application.port.out.GenerateurSequence;

/**
 * Numérotation annuelle par séquences PostgreSQL ({@code seq_<serie>_<annee>}), créées à la
 * première utilisation de l'année. Un verrou consultatif transactionnel sérialise la seule création
 * ; {@code nextval} garantit ensuite l'unicité sans verrou applicatif.
 */
@Component
public class SequencePostgresAdapter implements GenerateurSequence {

  private static final Pattern SERIE_VALIDE = Pattern.compile("[a-z_]{1,30}");

  private final JdbcTemplate jdbc;

  /**
   * @param jdbc accès JDBC (même connexion que la transaction en cours)
   */
  public SequencePostgresAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public long prochaineValeur(String serie, int annee) {
    if (!SERIE_VALIDE.matcher(serie).matches() || annee < 2000 || annee > 9999) {
      throw new IllegalArgumentException("Série de numérotation invalide : " + serie);
    }
    // Nom construit uniquement à partir de valeurs validées ci-dessus (pas d'injection possible).
    String sequence = "seq_" + serie + "_" + annee;
    Boolean absente = jdbc.queryForObject("SELECT to_regclass(?) IS NULL", Boolean.class, sequence);
    if (Boolean.TRUE.equals(absente)) {
      jdbc.queryForObject(
          "SELECT 1 FROM pg_advisory_xact_lock(hashtext(?))", Integer.class, sequence);
      jdbc.execute("CREATE SEQUENCE IF NOT EXISTS " + sequence);
    }
    Long valeur = jdbc.queryForObject("SELECT nextval(CAST(? AS regclass))", Long.class, sequence);
    if (valeur == null) {
      throw new IllegalStateException("Séquence " + sequence + " indisponible");
    }
    return valeur;
  }
}
