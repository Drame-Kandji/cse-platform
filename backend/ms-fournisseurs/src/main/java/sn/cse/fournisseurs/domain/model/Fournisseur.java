package sn.cse.fournisseurs.domain.model;

import sn.cse.common.exception.BusinessRuleException;

import java.time.Instant;
import java.util.UUID;

public class Fournisseur {
    private final UUID id;
    private String raisonSociale;
    private String ninea;
    private String rc;
    private String email;
    private String telephone;
    private String specialite;
    private String statut;
    private double scorePerformance;
    private boolean demandeBlacklistage;
    private String motifBlacklistage;
    private Instant createdAt;

    public Fournisseur(UUID id, String raisonSociale, String ninea, String rc, String email,
                       String telephone, String specialite, String statut, double scorePerformance,
                       boolean demandeBlacklistage, String motifBlacklistage, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.raisonSociale = raisonSociale;
        this.ninea = ninea;
        this.rc = rc;
        this.email = email;
        this.telephone = telephone;
        this.specialite = specialite;
        this.statut = statut != null ? statut : "EN_ATTENTE_VALIDATION";
        this.scorePerformance = scorePerformance;
        this.demandeBlacklistage = demandeBlacklistage;
        this.motifBlacklistage = motifBlacklistage;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void validerInscription() {
        if (!"EN_ATTENTE_VALIDATION".equals(this.statut)) {
            throw new BusinessRuleException("STATUT_INVALIDE", "Seul un fournisseur EN_ATTENTE_VALIDATION peut être validé");
        }
        this.statut = "ACTIF";
    }

    public void suspendre() {
        if ("BLACKLISTE".equals(this.statut)) {
            throw new BusinessRuleException("FOURNISSEUR_BLACKLISTE", "Un fournisseur blacklisté ne peut plus être modifié");
        }
        this.statut = "SUSPENDU";
    }

    public void demanderBlacklistage(String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessRuleException("MOTIF_REQUIS", "Le motif de demande de blacklistage est obligatoire (R09)");
        }
        this.demandeBlacklistage = true;
        this.motifBlacklistage = motif;
    }

    public void blacklisterParDg() {
        if (!this.demandeBlacklistage) {
            throw new BusinessRuleException("DEMANDE_REQUISE", "Le blacklistage par la DG requiert une demande préalable du Responsable Achats (R09)");
        }
        this.statut = "BLACKLISTE";
    }

    public double recalculerScore(double qualite, double delai, double conformite, double sav) {
        double nouveauScore = (qualite * 0.35) + (delai * 0.30) + (conformite * 0.20) + (sav * 0.15);
        this.scorePerformance = Math.round(((this.scorePerformance + nouveauScore) / 2.0) * 10.0) / 10.0;
        return this.scorePerformance;
    }

    public UUID getId() { return id; }
    public String getRaisonSociale() { return raisonSociale; }
    public String getNinea() { return ninea; }
    public String getRc() { return rc; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public String getSpecialite() { return specialite; }
    public String getStatut() { return statut; }
    public double getScorePerformance() { return scorePerformance; }
    public boolean isDemandeBlacklistage() { return demandeBlacklistage; }
    public String getMotifBlacklistage() { return motifBlacklistage; }
    public Instant getCreatedAt() { return createdAt; }
}

