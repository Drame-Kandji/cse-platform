package sn.cse.common.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bornes de pagination ({@code cse.pagination.*}).
 *
 * @param defaultSize taille par défaut
 * @param maxSize taille maximale (exigence : 50)
 */
@Validated
@ConfigurationProperties(prefix = "cse.pagination")
public record PaginationProperties(@Min(1) Integer defaultSize, @Min(1) @Max(500) Integer maxSize) {

  public PaginationProperties {
    defaultSize = defaultSize == null ? 20 : defaultSize;
    maxSize = maxSize == null ? 50 : maxSize;
  }
}
