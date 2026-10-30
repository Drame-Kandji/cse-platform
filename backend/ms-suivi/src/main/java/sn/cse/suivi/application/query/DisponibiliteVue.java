package sn.cse.suivi.application.query;

import java.util.List;

/**
 * Résultat d'une demande de disponibilité.
 *
 * @param disponible vrai si la quantité demandée est disponible
 * @param quantiteDemandee quantité demandée
 * @param quantiteDisponible quantité disponible
 * @param materiels matériels disponibles proposés
 */
public record DisponibiliteVue(
    boolean disponible,
    int quantiteDemandee,
    long quantiteDisponible,
    List<MaterielVue> materiels) {}
