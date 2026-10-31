package sn.cse.reporting.application.command;

import java.util.Objects;

/**
 * Fichier produit par un export.
 *
 * @param nom nom de fichier proposé ({@code tableau-de-bord-DG-2026-10-30.pdf})
 * @param typeMime type MIME
 * @param contenu octets du fichier
 */
public record FichierExporte(String nom, String typeMime, byte[] contenu) {

  public FichierExporte {
    Objects.requireNonNull(nom, "nom");
    Objects.requireNonNull(typeMime, "typeMime");
    contenu = contenu == null ? new byte[0] : contenu.clone();
  }

  @Override
  public byte[] contenu() {
    return contenu.clone();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof FichierExporte f
        && nom.equals(f.nom)
        && typeMime.equals(f.typeMime)
        && java.util.Arrays.equals(contenu, f.contenu);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nom, typeMime, java.util.Arrays.hashCode(contenu));
  }

  @Override
  public String toString() {
    return "FichierExporte[" + nom + ", " + typeMime + ", " + contenu.length + " octets]";
  }
}
