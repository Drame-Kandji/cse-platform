package sn.cse.notification.domain.model;

/**
 * Notification telle que vue par un utilisateur donné (avec son état de lecture).
 *
 * @param notification notification
 * @param lue vrai si cet utilisateur l'a marquée comme lue
 */
public record NotificationVue(Notification notification, boolean lue) {}
