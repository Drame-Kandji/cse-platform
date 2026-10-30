package sn.cse.besoins.application.command;

import sn.cse.common.security.CurrentUser;

/**
 * Soumission d'un besoin.
 *
 * @param contenu contenu saisi
 * @param utilisateur demandeur authentifié
 */
public record SoumettreBesoinCommand(ContenuBesoin contenu, CurrentUser utilisateur) {}
