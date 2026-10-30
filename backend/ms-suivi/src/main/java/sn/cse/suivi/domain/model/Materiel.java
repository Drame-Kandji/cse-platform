package sn.cse.suivi.domain.model;

import sn.cse.common.exception.BusinessRuleException;

import java.time.Instant;
import java.util.UUID;

public class Materiel {
    private final UUID id;
    private String immatriculation;
    private String designation;
    private String categorie;
    private EtatMateriel etat;
    private long heuresFonctionnement;
    private UUID chantierActuelId;
    private Instant createdAt;

    public Materiel(UUID id, String immatriculation, String designation, String categorie) {
        this.id = id != null ? id : UUID.randomUUID();
        this.immatriculation = immatriculation;
        this.designation = designation;
        this.categorie = categorie;
        this.etat = EtatMateriel.DISPONIBLE;
        this.heuresFonctionnement = 0;
        this.createdAt = Instant.now();
    }

    public static Materiel reconstituer(UUID id, String immatriculation, String designation, String categorie,
                                        EtatMateriel etat, long heuresFonctionnement, UUID chantierActuelId, Instant createdAt) {
        Materiel m = new Materiel(id, immatriculation, designation, categorie);
        m.etat = etat != null ? etat : EtatMateriel.DISPONIBLE;
        m.heuresFonctionnement = heuresFonctionnement;
        m.chantierActuelId = chantierActuelId;
        m.createdAt = createdAt != null ? createdAt : Instant.now();
        return m;
    }

    public void affecter(UUID chantierId) {
        if (etat == EtatMateriel.REFORME) {
            throw new BusinessRuleException("MATERIEL_REFORME", "Un matériel réformé ne peut pas être affecté");
        }
        if (etat == EtatMateriel.EN_MAINTENANCE) {
            throw new BusinessRuleException("MATERIEL_EN_MAINTENANCE", "Un matériel en maintenance ne peut pas être affecté");
        }
        if (etat == EtatMateriel.AFFECTE) {
            throw new BusinessRuleException("MATERIEL_DEJA_AFFECTE", "Ce matériel est déjà affecté");
        }
        this.etat = EtatMateriel.AFFECTE;
        this.chantierActuelId = chantierId;
    }

    public void liberer() {
        if (etat == EtatMateriel.REFORME) {
            throw new BusinessRuleException("MATERIEL_REFORME", "Un matériel réformé ne peut pas changer d'état");
        }
        this.etat = EtatMateriel.DISPONIBLE;
        this.chantierActuelId = null;
    }

    public void mettreEnMaintenance() {
        if (etat == EtatMateriel.REFORME) {
            throw new BusinessRuleException("MATERIEL_REFORME", "Un matériel réformé ne peut pas passer en maintenance");
        }
        if (etat == EtatMateriel.AFFECTE) {
            throw new BusinessRuleException("MATERIEL_AFFECTE", "Impossible de mettre en maintenance un matériel affecté sans le libérer au préalable");
        }
        this.etat = EtatMateriel.EN_MAINTENANCE;
    }

    public void sortirMaintenance() {
        if (etat != EtatMateriel.EN_MAINTENANCE) {
            throw new BusinessRuleException("ETAT_INVALIDE", "Le matériel doit être en maintenance pour en sortir");
        }
        this.etat = EtatMateriel.DISPONIBLE;
    }

    public void reformer() {
        if (etat == EtatMateriel.REFORME) {
            throw new BusinessRuleException("DEJA_REFORME", "Ce matériel est déjà réformé");
        }
        this.etat = EtatMateriel.REFORME;
        this.chantierActuelId = null;
    }

    public boolean estAffectable() {
        return etat == EtatMateriel.DISPONIBLE;
    }

    public UUID getId() { return id; }
    public String getImmatriculation() { return immatriculation; }
    public String getDesignation() { return designation; }
    public String getCategorie() { return categorie; }
    public EtatMateriel getEtat() { return etat; }
    public long getHeuresFonctionnement() { return heuresFonctionnement; }
    public UUID getChantierActuelId() { return chantierActuelId; }
    public Instant getCreatedAt() { return createdAt; }
}
