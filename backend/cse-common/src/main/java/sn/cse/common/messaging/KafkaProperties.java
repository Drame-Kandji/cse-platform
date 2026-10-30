package sn.cse.common.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres Kafka communs ({@code cse.kafka.*}).
 *
 * @param partitions partitions des topics créés
 * @param replicas facteur de réplication
 * @param retryInterval intervalle entre deux tentatives de consommation
 * @param retryAttempts tentatives avant envoi en Dead Letter Topic
 */
@ConfigurationProperties(prefix = "cse.kafka")
public record KafkaProperties(
    Integer partitions, Short replicas, Duration retryInterval, Long retryAttempts) {

  public KafkaProperties {
    partitions = partitions == null ? 3 : partitions;
    replicas = replicas == null ? 1 : replicas;
    retryInterval = retryInterval == null ? Duration.ofSeconds(2) : retryInterval;
    retryAttempts = retryAttempts == null ? 3L : retryAttempts;
  }
}
