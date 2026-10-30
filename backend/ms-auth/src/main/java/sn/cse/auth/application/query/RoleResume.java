package sn.cse.auth.application.query;

import sn.cse.auth.domain.model.Role;

/**
 * Synthèse d'un rôle pour l'écran d'administration.
 *
 * @param role rôle
 * @param nombreUtilisateurs nombre de profils portant ce rôle
 * @param otpObligatoire vrai si le second facteur est exigé
 */
public record RoleResume(Role role, long nombreUtilisateurs, boolean otpObligatoire) {}
