package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fournisseurs", schema = "fournisseurs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String raisonSociale;

    @Column(nullable = false, unique = true)
    private String ninea;

    private String rc;

    @Column(nullable = false)
    private String email;

    private String telephone;

    @Column(nullable = false)
    private String specialite;

    @Column(nullable = false)
    private String statut;

    private Double scorePerformance;
    private Boolean demandeBlacklistage;
    private String motifBlacklistage;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}

