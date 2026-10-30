package sn.cse.common.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Délais des appels HTTP inter-services ({@code cse.clients.*}).
 *
 * @param connectTimeout délai de connexion
 * @param readTimeout délai de lecture
 */
@ConfigurationProperties(prefix = "cse.clients")
public record ServiceClientProperties(Duration connectTimeout, Duration readTimeout) {

  public ServiceClientProperties {
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
    readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
  }
}
