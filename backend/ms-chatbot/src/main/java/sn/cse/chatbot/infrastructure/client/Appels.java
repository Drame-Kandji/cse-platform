package sn.cse.chatbot.infrastructure.client;

import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import sn.cse.chatbot.application.exception.SourceIndisponibleException;

/** Règles communes des appels sortants. */
final class Appels {

  private static final Logger LOG = LoggerFactory.getLogger(Appels.class);

  private Appels() {}

  /**
   * Une erreur 4xx (droit insuffisant, ressource absente, filtre refusé) n'est pas une panne : le
   * service appelé applique ses règles d'accès et le chatbot n'en voit pas plus. Elle donne une
   * liste vide et n'ouvre pas le disjoncteur.
   */
  static <T> List<T> lire(String service, Supplier<List<T>> appel) {
    try {
      List<T> r = appel.get();
      return r == null ? List.of() : r;
    } catch (HttpClientErrorException e) {
      LOG.debug("{} a répondu {} : aucune donnée retenue", service, e.getStatusCode());
      return List.of();
    }
  }

  static SourceIndisponibleException indisponible(String service, Throwable cause) {
    LOG.warn("Appel à {} impossible : {}", service, cause.toString());
    return cause instanceof SourceIndisponibleException s
        ? s
        : new SourceIndisponibleException(service, cause);
  }
}
