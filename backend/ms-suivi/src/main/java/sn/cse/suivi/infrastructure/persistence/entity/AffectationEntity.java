package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "affectations", schema = "suivi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectationEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID materielId;

    @Column(nullable = false)
    private UUID chantierId;

    private UUID besoinId;
    private UUID demandeurId;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFinPrevue;
    private LocalDate dateFinReelle;
    private String motif;
    private String affectePar;

    private Instant createdAt;

    @Version
    private Long version;
}
