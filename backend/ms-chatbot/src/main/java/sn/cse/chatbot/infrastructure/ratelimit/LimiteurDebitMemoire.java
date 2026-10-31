package sn.cse.chatbot.infrastructure.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import sn.cse.chatbot.application.port.out.LimiteurDebit;
import sn.cse.chatbot.infrastructure.config.ChatbotProperties;

/**
 * Seau de jetons en mémoire par utilisateur (une instance de service). La passerelle applique en
 * plus une limitation globale distribuée (Redis).
 */
@Component
public class LimiteurDebitMemoire implements LimiteurDebit {

  private static final int SEAUX_AVANT_NETTOYAGE = 10_000;

  private final Map<UUID, Seau> seaux = new ConcurrentHashMap<>();
  private final int capacite;
  private final Duration periode;
  private final Clock clock;

  /**
   * @param properties configuration
   * @param clock horloge
   */
  public LimiteurDebitMemoire(ChatbotProperties properties, Clock clock) {
    this.capacite = properties.limiteDebit().capacite();
    this.periode = properties.limiteDebit().periode();
    this.clock = clock;
  }

  @Override
  public boolean autoriser(UUID utilisateurId) {
    long maintenant = clock.millis();
    if (seaux.size() > SEAUX_AVANT_NETTOYAGE) {
      seaux.values().removeIf(s -> s.estPlein(maintenant));
    }
    return seaux.computeIfAbsent(utilisateurId, k -> new Seau(maintenant)).consommer(maintenant);
  }

  private final class Seau {
    private double jetons = capacite;
    private long derniereRecharge;

    Seau(long maintenant) {
      this.derniereRecharge = maintenant;
    }

    synchronized boolean consommer(long maintenant) {
      recharger(maintenant);
      if (jetons >= 1) {
        jetons -= 1;
        return true;
      }
      return false;
    }

    synchronized boolean estPlein(long maintenant) {
      recharger(maintenant);
      return jetons >= capacite;
    }

    private void recharger(long maintenant) {
      double ajout = (maintenant - derniereRecharge) * capacite / (double) periode.toMillis();
      jetons = Math.min(capacite, jetons + ajout);
      derniereRecharge = maintenant;
    }
  }
}
