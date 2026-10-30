package sn.cse.fournisseurs.application.query;

import java.math.BigDecimal;

/**
 * Statistiques globales du référentiel fournisseurs.
 *
 * @param total nombre total de fournisseurs
 * @param actifs fournisseurs ACTIF
 * @param enAttente fournisseurs EN_ATTENTE_VALIDATION
 * @param suspendus fournisseurs SUSPENDU
 * @param blacklistes fournisseurs BLACKLISTE
 * @param scoreMoyen score moyen des fournisseurs actifs (0 si aucun)
 * @param litigesOuverts litiges OUVERT ou EN_COURS
 * @param documentsExpires documents administratifs expirés
 */
public record StatistiquesFournisseurs(
    long total,
    long actifs,
    long enAttente,
    long suspendus,
    long blacklistes,
    BigDecimal scoreMoyen,
    long litigesOuverts,
    long documentsExpires) {}
