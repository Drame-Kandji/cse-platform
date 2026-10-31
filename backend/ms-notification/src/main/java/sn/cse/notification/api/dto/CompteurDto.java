package sn.cse.notification.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Nombre de notifications non lues.
 *
 * @param count nombre
 */
@Schema(name = "CompteurNonLues")
public record CompteurDto(@Schema(example = "3") long count) {}
