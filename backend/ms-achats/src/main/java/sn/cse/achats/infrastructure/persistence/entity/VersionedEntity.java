package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

/** Racine d'agrégat : audit + verrou optimiste. */
@Getter
@MappedSuperclass
public abstract class VersionedEntity extends AuditEntity {

  // Null tant que l'entité n'est pas persistée : Spring Data la reconnaît alors comme nouvelle.
  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
