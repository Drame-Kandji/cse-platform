package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;

/**
 * Critères de recherche des matériels.
 *
 * @param etat état (optionnel)
 * @param categorie catégorie (optionnelle)
 * @param q texte libre sur code, désignation, immatriculation, marque (optionnel)
 * @param maintenanceDue filtre sur l'échéance de maintenance (optionnel)
 * @param perimetre chantiers visibles (porte aussi le filtre {@code chantierId})
 */
public record FiltreMateriels(
    EtatMateriel etat,
    CategorieMateriel categorie,
    String q,
    Boolean maintenanceDue,
    Perimetre perimetre) {}
