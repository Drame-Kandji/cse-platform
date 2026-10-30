package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offres", schema = "achats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID appelOffreId;

    @Column(nullable = false)
    private UUID fournisseurId;

    @Column(nullable = false)
    private String codeAnonyme;

    @Column(nullable = false)
    private BigDecimal montantPropose;

    @Column(nullable = false)
    private Integer delaiLivraisonJours;

    private Double noteTechnique;
    private Instant createdAt;
}

