package sn.cse.auth.application.command;

import java.util.UUID;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur.Identite;

/**
 * Modification d'un compte par un administrateur.
 *
 * @param id compte modifié
 * @param identite nouvelle identité
 * @param role nouveau rôle
 * @param attributs nouveaux attributs
 * @param auteurId administrateur à l'origine de la demande
 */
public record ModifierUtilisateurCommand(
    UUID id, Identite identite, Role role, AttributsProfil attributs, UUID auteurId) {}
