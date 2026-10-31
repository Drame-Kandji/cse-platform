package sn.cse.audit.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.audit.application.port.in.ConsulterAuditUseCase;
import sn.cse.audit.application.port.out.JournalAuditRepository;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.audit.domain.model.ParametresAudit;
import sn.cse.audit.domain.model.StatistiquesAudit;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Consultation du journal d'audit : recherche filtrée et statistiques. */
@Service
@Transactional(readOnly = true)
public class ConsulterAuditService implements ConsulterAuditUseCase {

  private final JournalAuditRepository journal;
  private final ParametresAudit parametres;
  private final Clock clock;

  /**
   * @param journal persistance du journal
   * @param parametres paramètres de consultation
   * @param clock horloge
   */
  public ConsulterAuditService(
      JournalAuditRepository journal, ParametresAudit parametres, Clock clock) {
    this.journal = journal;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public PageResponse<AuditLog> rechercher(CritereRechercheAudit critere, PageQuery page) {
    critere.verifierTexteLibre(parametres.longueurMaxRecherche());
    return journal.rechercher(critere, page);
  }

  @Override
  public StatistiquesAudit statistiques() {
    LocalDate aujourdhui = LocalDate.now(clock);
    Instant debutJour = aujourdhui.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant debutFenetre =
        aujourdhui
            .minusDays(parametres.fenetreStatistiquesJours() - 1L)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);
    return new StatistiquesAudit(
        journal.compterTout(),
        journal.compterDepuis(debutJour),
        journal.compterDepuis(debutFenetre),
        journal.totalParService());
  }
}
