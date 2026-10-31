package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.DocumentPdf;
import sn.cse.achats.application.port.in.GenererProcesVerbalUseCase;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.ProcesVerbalGenerator;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.achats.domain.service.MoteurEvaluation;

/** Procès-verbaux : OUVERTURE après clôture, EVALUATION après attribution. */
@Service
@Transactional(readOnly = true)
public class ProcesVerbalService implements GenererProcesVerbalUseCase {

  private final AppelOffreRepository appelsOffres;
  private final ProcesVerbalGenerator generateur;
  private final MoteurEvaluation moteur;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param generateur générateur PDF
   * @param moteur moteur d'évaluation
   * @param clock horloge
   */
  public ProcesVerbalService(
      AppelOffreRepository appelsOffres,
      ProcesVerbalGenerator generateur,
      MoteurEvaluation moteur,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.generateur = generateur;
    this.moteur = moteur;
    this.clock = clock;
  }

  @Override
  public DocumentPdf generer(UUID appelOffreId, TypeProcesVerbal type) {
    AppelOffre appelOffre = Chargements.appelOffre(appelsOffres, appelOffreId);
    appelOffre.exigerProcesVerbalDisponible(type);
    byte[] pdf =
        generateur.generer(type, appelOffre, moteur.evaluer(appelOffre), Instant.now(clock));
    String nom =
        "PV-" + type.name().toLowerCase(Locale.ROOT) + "-" + appelOffre.getReference() + ".pdf";
    return new DocumentPdf(nom, pdf);
  }
}
