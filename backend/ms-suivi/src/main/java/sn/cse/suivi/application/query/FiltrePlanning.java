package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Periode;

/**
 * Critères du planning des affectations.
 *
 * @param categorie catégorie des matériels (optionnelle)
 * @param periode période affichée
 * @param perimetre chantiers visibles (porte aussi le filtre {@code chantierId})
 */
public record FiltrePlanning(CategorieMateriel categorie, Periode periode, Perimetre perimetre) {}
