package sn.cse.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.events.Topics;

/**
 * Consommation Kafka : création des topics (+ DLT), relecture des enveloppes, idempotence et
 * gestion d'erreur avec rejeu puis Dead Letter Topic.
 */
@AutoConfiguration(
    after = {
      KafkaAutoConfiguration.class,
      JdbcTemplateAutoConfiguration.class,
      CseCoreAutoConfiguration.class
    })
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class MessagingAutoConfiguration {

  /**
   * @param mapper sérialiseur
   * @return le lecteur d'enveloppes
   */
  @Bean
  public EventEnvelopeReader eventEnvelopeReader(ObjectMapper mapper) {
    return new EventEnvelopeReader(mapper);
  }

  /**
   * @param jdbc accès JDBC
   * @param clock horloge
   * @return le garde d'idempotence
   */
  @Bean
  @ConditionalOnBean(JdbcTemplate.class)
  public IdempotentEventHandler idempotentEventHandler(JdbcTemplate jdbc, Clock clock) {
    return new IdempotentEventHandler(jdbc, clock);
  }

  /**
   * @param properties paramètres
   * @return les topics de la plateforme et leurs DLT
   */
  @Bean
  public KafkaAdmin.NewTopics cseTopics(KafkaProperties properties) {
    return new KafkaAdmin.NewTopics(
        Stream.of(
                Topics.BESOINS,
                Topics.SUIVI,
                Topics.FOURNISSEURS,
                Topics.ACHATS,
                Topics.AUTH,
                Topics.AUDIT)
            .flatMap(t -> Stream.of(t, t + Topics.DLT_SUFFIX))
            .map(
                t ->
                    TopicBuilder.name(t)
                        .partitions(properties.partitions())
                        .replicas(properties.replicas())
                        .build())
            .toArray(org.apache.kafka.clients.admin.NewTopic[]::new));
  }

  /**
   * Rejoue un message en échec puis le publie sur {@code <topic>.DLT}. Les messages illisibles ne
   * sont pas rejoués.
   *
   * @param template producteur utilisé pour la DLT
   * @param properties paramètres de rejeu
   * @return le gestionnaire d'erreurs appliqué aux listeners
   */
  @Bean
  @ConditionalOnBean(KafkaOperations.class)
  public CommonErrorHandler cseKafkaErrorHandler(
      KafkaOperations<?, ?> template, KafkaProperties properties) {
    DefaultErrorHandler handler =
        new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(template),
            new FixedBackOff(properties.retryInterval().toMillis(), properties.retryAttempts()));
    handler.addNotRetryableExceptions(IllegalArgumentException.class);
    return handler;
  }
}
