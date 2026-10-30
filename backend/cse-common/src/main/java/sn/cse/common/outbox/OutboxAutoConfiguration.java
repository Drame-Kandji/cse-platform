package sn.cse.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.security.CurrentUserProvider;

/** Outbox transactionnelle : écriture (si JDBC) et relais planifié vers Kafka (si activé). */
@AutoConfiguration(
    after = {
      JdbcTemplateAutoConfiguration.class,
      KafkaAutoConfiguration.class,
      CseCoreAutoConfiguration.class
    })
@ConditionalOnClass({JdbcTemplate.class, KafkaTemplate.class})
@ConditionalOnBean(JdbcTemplate.class)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

  /**
   * @param jdbc accès JDBC
   * @param mapper sérialiseur
   * @param clock horloge
   * @param source nom du service
   * @return l'écrivain d'outbox
   */
  @Bean
  public OutboxWriter outboxWriter(
      JdbcTemplate jdbc,
      ObjectMapper mapper,
      Clock clock,
      @Value("${spring.application.name}") String source) {
    return new OutboxWriter(jdbc, mapper, new CurrentUserProvider(), clock, source);
  }

  /** Relais activé par {@code cse.outbox.relay-enabled} (vrai par défaut). */
  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  @ConditionalOnProperty(
      prefix = "cse.outbox",
      name = "relay-enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class Relais {

    @Bean
    OutboxRelay outboxRelay(
        JdbcTemplate jdbc,
        KafkaTemplate<String, String> kafka,
        PlatformTransactionManager txManager,
        OutboxProperties properties,
        Clock clock) {
      return new OutboxRelay(jdbc, kafka, new TransactionTemplate(txManager), properties, clock);
    }
  }
}
