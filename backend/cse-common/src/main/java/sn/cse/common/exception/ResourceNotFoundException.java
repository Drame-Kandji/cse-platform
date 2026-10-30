package sn.cse.common.exception;

/** Ressource introuvable : HTTP 404. */
public class ResourceNotFoundException extends CseException {

  /**
   * @param entite nom métier de l'entité (ex. {@code "Besoin"})
   * @param id identifiant recherché
   */
  public ResourceNotFoundException(String entite, Object id) {
    super(
        entite.toUpperCase(java.util.Locale.ROOT) + "_INTROUVABLE",
        entite + " introuvable : " + id);
  }

  @Override
  public int status() {
    return 404;
  }
}
