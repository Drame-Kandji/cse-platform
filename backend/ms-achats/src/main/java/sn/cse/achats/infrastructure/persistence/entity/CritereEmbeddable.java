package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.achats.domain.model.CritereEvaluation;

/** Critère pondéré d'un appel d'offres. */
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class CritereEmbeddable {

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "critere", nullable = false, columnDefinition = "critere_evaluation")
  private CritereEvaluation critere;

  @Column(name = "poids", nullable = false)
  private int poids;
}
