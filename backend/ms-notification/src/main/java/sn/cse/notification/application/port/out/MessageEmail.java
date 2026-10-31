package sn.cse.notification.application.port.out;

import sn.cse.notification.domain.model.NiveauNotification;

/**
 * E-mail de notification prêt à être mis en forme.
 *
 * @param adresse adresse du destinataire
 * @param nomDestinataire nom affiché dans la salutation
 * @param titre titre de la notification (sert aussi d'objet)
 * @param message message
 * @param niveau criticité (couleur du bandeau)
 * @param lien route frontend relative, ou {@code null}
 */
public record MessageEmail(
    String adresse,
    String nomDestinataire,
    String titre,
    String message,
    NiveauNotification niveau,
    String lien) {}
