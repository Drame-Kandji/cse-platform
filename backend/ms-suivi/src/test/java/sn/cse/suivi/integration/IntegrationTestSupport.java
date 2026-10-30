package sn.cse.suivi.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import sn.cse.common.test.PostgresTestConfiguration;

/**
 * Contexte commun aux tests d'intégration (PostgreSQL Testcontainers, sans Kafka). Le profil {@code
 * dev} active l'amorçage : il est ainsi vérifié à chaque démarrage. Une configuration identique
 * permet de partager un seul contexte Spring entre les classes de test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Import(PostgresTestConfiguration.class)
abstract class IntegrationTestSupport {}
