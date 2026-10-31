package sn.cse.reporting.domain.model;

import java.util.Locale;
import sn.cse.common.exception.InvalidRequestException;

/** Formats d'export du tableau de bord. */
public enum FormatExport {
  /** Document PDF (OpenPDF). */
  PDF("application/pdf", "pdf"),
  /** Classeur Excel (Apache POI). */
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

  private final String typeMime;
  private final String extension;

  FormatExport(String typeMime, String extension) {
    this.typeMime = typeMime;
    this.extension = extension;
  }

  /**
   * @return type MIME du fichier
   */
  public String typeMime() {
    return typeMime;
  }

  /**
   * @return extension du fichier, sans point
   */
  public String extension() {
    return extension;
  }

  /**
   * @param code {@code pdf} ou {@code xlsx}, insensible à la casse
   * @return le format
   * @throws InvalidRequestException si le format est inconnu
   */
  public static FormatExport depuis(String code) {
    String c = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    for (FormatExport f : values()) {
      if (f.name().equals(c)) {
        return f;
      }
    }
    throw new InvalidRequestException(
        "FORMAT_INCONNU", "Format d'export « " + code + " » inconnu. Valeurs : pdf, xlsx.");
  }
}
