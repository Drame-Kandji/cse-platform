package sn.cse.besoins.domain.model;

import sn.cse.common.exception.BusinessRuleException;

import java.time.Instant;
import java.util.UUID;

public class BesoinMateriel {
    private final UUID id;
    private String reference;
    private String designation;
    private int quantite;
    private String specifications;
    private Priorite priorite;
    private StatutBesoin statut;
    private UUID chantierId;
    private UUID demandeurId;
    private String justification;
    private String motifRejet;
    private Instant createdAt;

    public BesoinMateriel(UUID id, String designation, int quantite, Priorite priorite, UUID chantierId, UUID demandeurId, String justification) {
        if ((priorite == Priorite.URGENT || priorite == Priorite.CRITIQUE) && (justification == null || justification.isBlank())) {
            throw new BusinessRuleException("JUSTIFICATION_REQUISE", "Une justification est obligatoire pour un besoin URGENT ou CRITIQUE (R08)");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.reference = "BES-" + System.currentTimeMillis();
        this.designation = designation;
        this.quantite = quantite;
        this.priorite = priorite != null ? priorite : Priorite.NORMAL;
        this.statut = StatutBesoin.EN_ATTENTE;
        this.chantierId = chantierId;
        this.demandeurId = demandeurId;
        this.justification = justification;
        this.createdAt = Instant.now();
    }

    public static BesoinMateriel reconstituer(UUID id, String reference, String designation, int quantite,
                                              String specifications, Priorite priorite, StatutBesoin statut,
                                              UUID chantierId, UUID demandeurId, String justification,
                                              String motifRejet, Instant createdAt) {
        BesoinMateriel b = new BesoinMateriel(
                id,
                designation,
                quantite,
                Priorite.NORMAL,
                chantierId,
                demandeurId,
                justification
        );
        b.reference = reference;
        b.specifications = specifications;
        b.priorite = priorite != null ? priorite : Priorite.NORMAL;
        b.statut = statut != null ? statut : StatutBesoin.EN_ATTENTE;
        b.motifRejet = motifRejet;
        b.createdAt = createdAt != null ? createdAt : Instant.now();
        return b;
    }

    public void valider() {
        if (statut != StatutBesoin.EN_ATTENTE) {
            throw new BusinessRuleException("BESOIN_NON_VALIDABLE", "Seul un besoin en attente peut être validé (Statut actuel: " + statut + ")");
        }
        this.statut = StatutBesoin.VALIDE;
    }

    public void rejeter(String motif) {
        if (statut != StatutBesoin.EN_ATTENTE) {
            throw new BusinessRuleException("BESOIN_NON_REJETABLE", "Seul un besoin en attente peut être rejeté (Statut actuel: " + statut + ")");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessRuleException("MOTIF_REJET_REQUIS", "Un motif est obligatoire pour le rejet (R08)");
        }
        this.statut = StatutBesoin.REJETE;
        this.motifRejet = motif;
    }

    public void marquerAffecteDirect() {
        if (statut != StatutBesoin.VALIDE) {
            throw new BusinessRuleException("TRANSITION_INVALIDE", "Le besoin doit être validé pour être affecté directement");
        }
        this.statut = StatutBesoin.AFFECTE_DIRECT;
    }

    public void marquerEnAchat() {
        if (statut != StatutBesoin.VALIDE) {
            throw new BusinessRuleException("TRANSITION_INVALIDE", "Le besoin doit être validé pour passer en achat");
        }
        this.statut = StatutBesoin.EN_ACHAT;
    }

    public boolean estModifiable() {
        return statut == StatutBesoin.EN_ATTENTE;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getDesignation() { return designation; }
    public int getQuantite() { return quantite; }
    public String getSpecifications() { return specifications; }
    public Priorite getPriorite() { return priorite; }
    public StatutBesoin getStatut() { return statut; }
    public UUID getChantierId() { return chantierId; }
    public UUID getDemandeurId() { return demandeurId; }
    public String getJustification() { return justification; }
    public String getMotifRejet() { return motifRejet; }
    public Instant getCreatedAt() { return createdAt; }
}
