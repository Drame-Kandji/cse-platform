package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.port.in.SurveillanceFournisseursUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.model.DocumentAdministratif;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/** Contrôles périodiques : documents expirés et scores sous le seuil d'alerte. */
@Service
@Transactional
public class SurveillanceService implements SurveillanceFournisseursUseCase {

  private final FournisseurRepository fournisseurs;
  private final ScorePolicy politique;
  private final FournisseurEventPublisher evenements;
  private final Clock clock;

  /**
   * @param fournisseurs dépôt
   * @param politique politique de score (seuil)
   * @param evenements publication outbox
   * @param clock horloge
   */
  public SurveillanceService(
      FournisseurRepository fournisseurs,
      ScorePolicy politique,
      FournisseurEventPublisher evenements,
      Clock clock) {
    this.fournisseurs = fournisseurs;
    this.politique = politique;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public int signalerDocumentsExpires() {
    LocalDate aujourdhui = LocalDate.now(clock);
    int total = 0;
    for (Fournisseur fournisseur : fournisseurs.avecDocumentsExpiresNonSignales(aujourdhui)) {
      List<DocumentAdministratif> expires = fournisseur.signalerDocumentsExpires(aujourdhui);
      if (expires.isEmpty()) {
        continue;
      }
      Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
      for (DocumentAdministratif document : expires) {
        evenements.publierFournisseur(EventTypes.DOCUMENT_EXPIRE, persiste, motif(document));
      }
      total += expires.size();
    }
    return total;
  }

  @Override
  public int alerterScoresFaibles() {
    int alertes = 0;
    for (Fournisseur fournisseur : fournisseurs.parStatut(StatutFournisseur.ACTIF)) {
      if (politique.estSousSeuilAlerte(fournisseur.getPerformance().scorePerformance())) {
        evenements.publierFournisseur(
            EventTypes.SCORE_RECALCULE,
            fournisseur,
            "ALERTE : score "
                + fournisseur.getPerformance().scorePerformance().toPlainString()
                + " inférieur au seuil d'alerte "
                + politique.seuilAlerte().toPlainString());
        alertes++;
      }
    }
    return alertes;
  }

  private static String motif(DocumentAdministratif document) {
    return "Document "
        + document.getType()
        + " « "
        + document.getLibelle()
        + " » expiré le "
        + document.getDateExpiration().map(LocalDate::toString).orElse("?");
  }
}
