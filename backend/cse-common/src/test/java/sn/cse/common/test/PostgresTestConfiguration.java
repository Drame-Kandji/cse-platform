package sn.cse.common.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base PostgreSQL 15 éphémère pour les tests d'intégration ({@code @Import} dans le test). Le
 * conteneur est partagé par tous les contextes de test d'une même JVM.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15-alpine").withReuse(false);

  /**
   * @return le conteneur, câblé automatiquement comme DataSource
   */
  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgres() {
    return POSTGRES;
  }
}
