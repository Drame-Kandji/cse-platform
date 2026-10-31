package sn.cse.achats.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.EvaluerOffreCommand;
import sn.cse.achats.application.port.in.EvaluerOffresUseCase;
import sn.cse.achats.application.port.in.OffreVue;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.service.Comparatif;
import sn.cse.achats.domain.service.MoteurEvaluation;
import sn.cse.achats.domain.service.OffreEvaluee;

/** Saisie des notes et comparatif multicritère (stratégies de notation du domaine). */
@Service
@Transactional
public class EvaluationOffresService implements EvaluerOffresUseCase {

  private final AppelOffreRepository appelsOffres;
  private final MoteurEvaluation moteur;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param moteur moteur d'évaluation
   */
  public EvaluationOffresService(AppelOffreRepository appelsOffres, MoteurEvaluation moteur) {
    this.appelsOffres = appelsOffres;
    this.moteur = moteur;
  }

  @Override
  public OffreVue evaluer(EvaluerOffreCommand commande) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, commande.appelOffreId());
    appelOffre.evaluerOffre(commande.offreId(), commande.notes());
    AppelOffre enregistre = appelsOffres.enregistrer(appelOffre);
    OffreEvaluee evaluee =
        moteur.evaluer(enregistre).stream()
            .filter(e -> e.offre().getId().equals(commande.offreId()))
            .findFirst()
            .orElseThrow();
    return new OffreVue(enregistre.getId(), enregistre.estAnonyme(), evaluee);
  }

  @Override
  @Transactional(readOnly = true)
  public Comparatif comparatif(UUID appelOffreId) {
    return moteur.comparer(Chargements.appelOffre(appelsOffres, appelOffreId));
  }
}
