package sn.cse.audit.domain.exception;

/**
 * Événement d'audit incomplet ou hors bornes. Hérite de {@link IllegalArgumentException} afin que
 * le gestionnaire d'erreurs Kafka ne le rejoue pas et l'envoie directement en Dead Letter Topic :
 * un rejeu ne le rendrait pas valide.
 */
public class EntreeAuditInvalideException extends IllegalArgumentException {

  /**
   * @param message explication en français
   */
  public EntreeAuditInvalideException(String message) {
    super(message);
  }
}
