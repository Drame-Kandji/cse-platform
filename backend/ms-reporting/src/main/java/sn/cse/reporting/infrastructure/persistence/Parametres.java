package sn.cse.reporting.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Paramètres SQL typés (les valeurs nulles gardent leur type JDBC) et lecture typée des colonnes.
 */
final class Parametres extends MapSqlParameterSource {

  Parametres uuid(String nom, UUID valeur) {
    addValue(nom, valeur, Types.OTHER);
    return this;
  }

  Parametres texte(String nom, String valeur) {
    addValue(nom, valeur, Types.VARCHAR);
    return this;
  }

  Parametres enumeration(String nom, Enum<?> valeur) {
    addValue(nom, valeur == null ? null : valeur.name(), Types.VARCHAR);
    return this;
  }

  Parametres date(String nom, LocalDate valeur) {
    addValue(nom, valeur, Types.DATE);
    return this;
  }

  Parametres instant(String nom, Instant valeur) {
    addValue(
        nom,
        valeur == null ? null : OffsetDateTime.ofInstant(valeur, ZoneOffset.UTC),
        Types.TIMESTAMP_WITH_TIMEZONE);
    return this;
  }

  Parametres montant(String nom, BigDecimal valeur) {
    addValue(nom, valeur, Types.NUMERIC);
    return this;
  }

  Parametres entier(String nom, long valeur) {
    addValue(nom, valeur, Types.BIGINT);
    return this;
  }

  Parametres booleen(String nom, boolean valeur) {
    addValue(nom, valeur, Types.BOOLEAN);
    return this;
  }

  static UUID uuid(ResultSet rs, String colonne) throws SQLException {
    return rs.getObject(colonne, UUID.class);
  }

  static Instant instant(ResultSet rs, String colonne) throws SQLException {
    OffsetDateTime v = rs.getObject(colonne, OffsetDateTime.class);
    return v == null ? null : v.toInstant();
  }

  static LocalDate date(ResultSet rs, String colonne) throws SQLException {
    return rs.getObject(colonne, LocalDate.class);
  }

  static <E extends Enum<E>> E enumeration(ResultSet rs, String colonne, Class<E> type)
      throws SQLException {
    String v = rs.getString(colonne);
    return v == null ? null : Enum.valueOf(type, v);
  }
}
