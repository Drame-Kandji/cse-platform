package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Table {@code contrat_jalon}. */
@Getter
@Setter
@Entity
@Table(name = "contrat_jalon")
public class JalonEntity extends AuditEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contrat_id", nullable = false, updatable = false)
  private ContratAchatEntity contrat;

  private int ordre;

  @Column(nullable = false)
  private String libelle;

  @Column(nullable = false)
  private LocalDate datePrevue;

  private LocalDate dateReelle;
}
