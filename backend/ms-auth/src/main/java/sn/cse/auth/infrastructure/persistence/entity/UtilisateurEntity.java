package sn.cse.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurEntity {
    @Id
    private UUID id;

    private String keycloakId;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String typeUtilisateur;

    private Boolean actif;
    private UUID chantierAttribueId;
    private String perimetreParc;
    private BigDecimal seuilDelegation;
    private BigDecimal budgetGere;
    private UUID fournisseurId;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}

