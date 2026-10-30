package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "appels_offres", schema = "achats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppelOffreEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String objet;

    @Column(nullable = false)
    private String typeAo;

    private UUID besoinId;

    @Column(nullable = false)
    private LocalDate dateLimite;

    @Column(nullable = false)
    private String statut;

    private UUID offreRetenueId;
    private Instant createdAt;

    @Version
    private Long version;
}

