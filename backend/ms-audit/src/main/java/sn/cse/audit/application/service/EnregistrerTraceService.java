package sn.cse.audit.application.service;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.audit.application.port.in.EnregistrerTraceUseCase;
import sn.cse.audit.application.port.out.JournalAuditRepository;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.TraceAction;

/**
 * Ajoute une action au journal. Double garde d'idempotence : la table {@code processed_event}
 * (consommateur Kafka) et l'unicité de {@code event_id} vérifiée ici, ce qui protège aussi d'un
 * second consommateur ou d'un rejeu manuel.
 */
@Service
public class EnregistrerTraceService implements EnregistrerTraceUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(EnregistrerTraceService.class);

  private final JournalAuditRepository journal;
  private final Clock clock;

  /**
   * @param journal persistance du journal
   * @param clock horloge
   */
  public EnregistrerTraceService(JournalAuditRepository journal, Clock clock) {
    this.journal = journal;
    this.clock = clock;
  }

  @Override
  @Transactional
  public boolean enregistrer(TraceAction trace) {
    if (journal.existePourEvenement(trace.eventId())) {
      LOG.debug("Événement d'audit {} déjà journalisé, ignoré", trace.eventId());
      return false;
    }
    journal.ajouter(AuditLog.nouvelle(trace, Instant.now(clock)));
    return true;
  }
}
