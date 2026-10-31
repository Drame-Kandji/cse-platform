package sn.cse.notification.api.sse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Flux SSE ({@code cse.notification.sse.*}).
 *
 * @param pingInterval intervalle des événements {@code ping}
 * @param timeout durée de vie d'une connexion (le client se reconnecte ensuite)
 * @param maxConnexionsParUtilisateur connexions simultanées par utilisateur (onglets)
 * @param reconnexion délai de reconnexion conseillé au client
 */
@Validated
@ConfigurationProperties(prefix = "cse.notification.sse")
public record SseProperties(
    @NotNull Duration pingInterval,
    @NotNull Duration timeout,
    @Min(1) int maxConnexionsParUtilisateur,
    @NotNull Duration reconnexion) {}
