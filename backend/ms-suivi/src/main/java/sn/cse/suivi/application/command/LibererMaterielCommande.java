package sn.cse.suivi.application.command;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Libération (retour au parc) d'un matériel.
 *
 * @param materielId matériel
 * @param dateFinReelle date de fin effective (défaut : aujourd'hui)
 * @param commentaire commentaire
 * @param auteur utilisateur
 */
public record LibererMaterielCommande(
    UUID materielId, LocalDate dateFinReelle, String commentaire, CurrentUser auteur) {}
