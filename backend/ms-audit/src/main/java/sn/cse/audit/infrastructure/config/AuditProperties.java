package sn.cse.audit.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Paramètres de ms-audit ({@code cse.audit.*}).
 *
 * @param fenetreStatistiquesJours fenêtre glissante des statistiques (7 = « sept derniers jours »)
 * @param longueurMaxRecherche longueur maximale du texte libre {@code q}
 * @param consommateur nom logique du consommateur pour l'idempotence ({@code processed_event})
 */
@Validated
@ConfigurationProperties(prefix = "cse.audit")
public record AuditProperties(
    @NotNull @Min(1) @Max(366) Integer fenetreStatistiquesJours,
    @NotNull @Min(1) @Max(500) Integer longueurMaxRecherche,
    @NotNull String consommateur) {}
