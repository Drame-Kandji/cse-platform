package sn.cse.achats.application.port.in;

import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.Offre;

/**
 * Offre d'un fournisseur vue depuis le portail, avec son appel d'offres.
 *
 * @param appelOffre appel d'offres
 * @param offre offre du fournisseur connecté
 */
public record OffreFournisseur(AppelOffre appelOffre, Offre offre) {}
