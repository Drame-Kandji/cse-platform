package sn.cse.notification.application.port.out;

/** Envoi d'e-mails. Les implémentations ne lèvent pas d'exception : un échec est journalisé. */
public interface MailPort {

  /**
   * @param message e-mail à envoyer
   * @return vrai si l'envoi a réussi
   */
  boolean envoyer(MessageEmail message);
}
