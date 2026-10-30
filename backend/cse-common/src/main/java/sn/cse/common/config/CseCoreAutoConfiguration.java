package sn.cse.common.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** Beans transverses : horloge injectable (testabilité des règles datées). */
@AutoConfiguration
public class CseCoreAutoConfiguration {

  /**
   * @return horloge UTC (le Sénégal est en UTC+0 toute l'année)
   */
  @Bean
  @ConditionalOnMissingBean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
