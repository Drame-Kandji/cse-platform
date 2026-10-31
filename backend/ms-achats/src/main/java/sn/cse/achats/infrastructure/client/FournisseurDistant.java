package sn.cse.achats.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Sous-ensemble de {@code FournisseurDetail} renvoyé par {@code GET /api/fournisseurs/{id}}.
 *
 * @param id identifiant
 * @param raisonSociale raison sociale
 * @param statut statut (EN_ATTENTE_VALIDATION, ACTIF, SUSPENDU, BLACKLISTE)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FournisseurDistant(UUID id, String raisonSociale, String statut) {}
