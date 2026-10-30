package sn.cse.auth.infrastructure.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sn.cse.auth.application.port.out.CompensationTransactionnelle;

/** Compensation déclenchée par la synchronisation de transaction Spring en cas de rollback. */
@Component
public class SpringCompensationTransactionnelle implements CompensationTransactionnelle {

  private static final Logger LOG =
      LoggerFactory.getLogger(SpringCompensationTransactionnelle.class);

  @Override
  public void siAnnulation(Runnable compensation) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      throw new IllegalStateException("Compensation demandée hors transaction");
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              executer(compensation);
            }
          }
        });
  }

  private static void executer(Runnable compensation) {
    try {
      compensation.run();
    } catch (RuntimeException e) {
      // La transaction est déjà annulée : on journalise l'échec pour reprise manuelle.
      LOG.error("Échec de la compensation après annulation de transaction", e);
    }
  }
}
