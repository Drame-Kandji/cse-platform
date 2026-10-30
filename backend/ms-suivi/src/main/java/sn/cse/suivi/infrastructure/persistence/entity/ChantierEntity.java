package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "chantiers", schema = "suivi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChantierEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String nom;

    private String adresse;
    private String ville;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private String statut;

    private BigDecimal budget;
    private String maitreOuvrage;
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @Version
    private Long version;
}
