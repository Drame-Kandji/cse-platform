package sn.cse.suivi.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.in.ConsulterAffectationsUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.application.query.FiltreAffectations;
import sn.cse.suivi.application.query.FiltrePlanning;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.StatutAffectation;

/** Consultation des affectations et du planning dans le périmètre de l'utilisateur. */
@Service
@Transactional(readOnly = true)
public class ConsulterAffectationsService implements ConsulterAffectationsUseCase {

  private final AffectationRepository affectations;
  private final AssembleurVues vues;
  private final SuiviParametres parametres;
  private final Clock clock;

  /**
   * @param affectations stockage des affectations
   * @param vues assembleur de vues
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public ConsulterAffectationsService(
      AffectationRepository affectations,
      AssembleurVues vues,
      SuiviParametres parametres,
      Clock clock) {
    this.affectations = affectations;
    this.vues = vues;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public PageResponse<AffectationVue> rechercher(
      UUID chantierId,
      UUID materielId,
      StatutAffectation statut,
      PageQuery page,
      CurrentUser lecteur) {
    FiltreAffectations filtre =
        new FiltreAffectations(materielId, statut, Perimetre.de(lecteur).restreindreA(chantierId));
    return AssembleurVues.page(affectations.rechercher(filtre, page), vues::affectations);
  }

  @Override
  public List<AffectationVue> planning(
      UUID chantierId,
      CategorieMateriel categorie,
      LocalDate du,
      LocalDate au,
      CurrentUser lecteur) {
    LocalDate aujourdhui = LocalDate.now(clock);
    SuiviParametres.Planning config = parametres.planning();
    LocalDate debut = du == null ? aujourdhui.minusDays(config.joursAvantParDefaut()) : du;
    LocalDate fin = au == null ? aujourdhui.plusDays(config.joursApresParDefaut()) : au;
    if (fin.isBefore(debut)) {
      throw new InvalidRequestException(
          Periode.PERIODE_INVALIDE, "La date « au » doit être postérieure à la date « du ».");
    }
    Periode periode = new Periode(debut, fin);
    if (periode.dureeJours() > config.maxJours()) {
      throw new InvalidRequestException(
          "PERIODE_TROP_LONGUE",
          "Le planning est limité à " + config.maxJours() + " jours par requête.");
    }
    FiltrePlanning filtre =
        new FiltrePlanning(categorie, periode, Perimetre.de(lecteur).restreindreA(chantierId));
    return vues.affectations(affectations.planning(filtre));
  }
}
