package sn.cse.suivi.application.command;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Affectation d'un matériel à un chantier.
 *
 * @param materielId matériel
 * @param chantierId chantier de destination
 * @param besoinId besoin d'origine (optionnel)
 * @param dateDebut début prévu
 * @param dateFinPrevue fin prévue
 * @param auteur utilisateur
 */
public record AffecterMaterielCommande(
    UUID materielId,
    UUID chantierId,
    UUID besoinId,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    CurrentUser auteur) {}
