package sn.cse.auth.application.port.out;

import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Utilisateur.Identite;

/**
 * Valeurs synchronisées vers le fournisseur d'identité lors d'une modification de profil.
 *
 * @param identite identité
 * @param attributs attributs spécifiques du rôle
 */
public record ProfilIdentite(Identite identite, AttributsProfil attributs) {}
