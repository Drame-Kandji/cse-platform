package sn.cse.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public abstract class Utilisateur {
    private UUID id;
    private String keycloakId;
    private String nom;
    private String prenom;
    private String email;
    private boolean actif;
    private Instant createdAt;

    protected Utilisateur(UUID id, String nom, String prenom, String email) {
        this.id = id != null ? id : UUID.randomUUID();
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.actif = true;
        this.createdAt = Instant.now();
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public void activer() { this.actif = true; }
    public void desactiver() { this.actif = false; }

    public UUID getId() { return id; }
    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public boolean isActif() { return actif; }
    public Instant getCreatedAt() { return createdAt; }
}
