package sn.cse.suivi.application.query;

import java.util.List;

/**
 * Données de la carte du parc.
 *
 * @param chantiers chantiers visibles
 * @param materiels matériels géolocalisés
 */
public record CarteVue(List<ChantierVue> chantiers, List<MaterielVue> materiels) {}
