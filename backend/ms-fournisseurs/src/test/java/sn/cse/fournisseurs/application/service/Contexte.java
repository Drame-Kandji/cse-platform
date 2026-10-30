package sn.cse.fournisseurs.application.service;

import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.service.ScorePolicy;
import sn.cse.fournisseurs.support.Fixtures;
import sn.cse.fournisseurs.support.Memoire;

/** Câblage des cas d'utilisation sur les adaptateurs en mémoire. */
class Contexte {

  final Memoire.Fournisseurs fournisseurs = new Memoire.Fournisseurs();
  final Memoire.Evaluations evaluations = new Memoire.Evaluations();
  final Memoire.Litiges litiges = new Memoire.Litiges();
  final Memoire.Catalogue catalogue = new Memoire.Catalogue();
  final Memoire.Publications publications = new Memoire.Publications();
  final ScorePolicy politique = Fixtures.politique();
  final ChargeurFournisseur chargeur =
      new ChargeurFournisseur(fournisseurs, litiges, Fixtures.HORLOGE);
  final RecalculScore recalcul =
      new RecalculScore(fournisseurs, evaluations, litiges, politique, publications);
  final ReferentielService referentiel =
      new ReferentielService(fournisseurs, chargeur, politique, publications);
  final CycleVieService cycleVie =
      new CycleVieService(fournisseurs, chargeur, publications, Fixtures.HORLOGE);
  final ConsultationService consultation = new ConsultationService(fournisseurs, litiges, chargeur);
  final EvaluationService evaluation =
      new EvaluationService(evaluations, chargeur, politique, recalcul, Fixtures.HORLOGE);
  final PerformanceService performance = new PerformanceService(fournisseurs, recalcul);
  final SurveillanceService surveillance =
      new SurveillanceService(fournisseurs, politique, publications, Fixtures.HORLOGE);
  final CatalogueService catalogues =
      new CatalogueService(catalogue, fournisseurs, chargeur, Fixtures.HORLOGE);
  final LitigeService litigeService =
      new LitigeService(
          litiges, chargeur, new Memoire.References(), recalcul, publications, Fixtures.HORLOGE);

  Fournisseur enregistrer(Fournisseur f) {
    return fournisseurs.sauvegarder(f);
  }
}
