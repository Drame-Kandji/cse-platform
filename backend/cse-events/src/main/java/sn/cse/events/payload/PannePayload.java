package sn.cse.events.payload;

import java.util.UUID;

/** Signalement de panne par un chef de chantier ou le responsable matériel (PANNE_SIGNALEE). */
public record PannePayload(
    UUID panneId,
    UUID materielId,
    String materielDesignation,
    UUID chantierId,
    String chantierNom,
    String gravite,
    String description,
    UUID signaleParId,
    String signaleParNom) {}
