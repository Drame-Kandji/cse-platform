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
@Table(name = "contrats_achat", schema = "achats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratAchatEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private UUID appelOffreId;

    private UUID offreId;

    @Column(nullable = false)
    private UUID fournisseurId;

    @Column(nullable = false)
    private BigDecimal montantTotal;

    @Column(nullable = false)
    private String statut;

    private Boolean engagementValideDaf;
    private Instant createdAt;

    @Version
    private Long version;
}

