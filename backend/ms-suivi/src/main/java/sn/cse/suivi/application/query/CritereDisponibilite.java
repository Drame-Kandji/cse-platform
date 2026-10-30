package sn.cse.suivi.application.query;

import java.time.LocalDate;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Demande de disponibilité (utilisée par ms-besoins lors de la validation d'un besoin).
 *
 * @param categorie catégorie (optionnelle)
 * @param designation fragment de désignation (optionnel)
 * @param quantite quantité souhaitée (≥ 1, défaut 1)
 * @param du début de la période d'utilisation (optionnel)
 * @param au fin de la période d'utilisation (optionnel)
 */
public record CritereDisponibilite(
    CategorieMateriel categorie,
    String designation,
    Integer quantite,
    LocalDate du,
    LocalDate au) {}
