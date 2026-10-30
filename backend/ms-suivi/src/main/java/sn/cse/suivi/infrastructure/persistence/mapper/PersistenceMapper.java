package sn.cse.suivi.infrastructure.persistence.mapper;

import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.infrastructure.persistence.entity.AffectationEntity;
import sn.cse.suivi.infrastructure.persistence.entity.ChantierEntity;
import sn.cse.suivi.infrastructure.persistence.entity.MaintenanceEntity;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;
import sn.cse.suivi.infrastructure.persistence.entity.MouvementEntity;
import sn.cse.suivi.infrastructure.persistence.entity.PanneEntity;
import sn.cse.suivi.infrastructure.persistence.entity.TypeEnginEntity;

/**
 * Conversion manuelle entité ↔ domaine. La version est recopiée : une entité reconstruite avec une
 * version obsolète échoue à la fusion (verrou optimiste).
 */
public final class PersistenceMapper {

  private PersistenceMapper() {}

  /**
   * @param t type d'engin
   * @return l'entité
   */
  public static TypeEnginEntity versEntite(TypeEngin t) {
    TypeEnginEntity e = new TypeEnginEntity();
    DonneesTypeEngin d = t.getDonnees();
    e.setId(t.getId());
    e.setCode(d.code());
    e.setLibelle(d.libelle());
    e.setCategorie(d.categorie());
    e.setSeuilMaintenanceHeures(d.seuilMaintenanceHeures());
    e.setSeuilMaintenanceJours(d.seuilMaintenanceJours());
    e.setActif(d.actif());
    e.setVersion(t.getVersion());
    return e;
  }

  /**
   * @param e entité
   * @return le type d'engin
   */
  public static TypeEngin versDomaine(TypeEnginEntity e) {
    return TypeEngin.reconstituer(
        new TypeEngin.Snapshot(
            e.getId(),
            new DonneesTypeEngin(
                e.getCode(),
                e.getLibelle(),
                e.getCategorie(),
                e.getSeuilMaintenanceHeures(),
                e.getSeuilMaintenanceJours(),
                e.isActif()),
            e.getVersion()));
  }

  /**
   * @param c chantier
   * @return l'entité
   */
  public static ChantierEntity versEntite(Chantier c) {
    ChantierEntity e = new ChantierEntity();
    e.setId(c.getId());
    e.setCode(c.getCode());
    e.setNom(c.getNom());
    e.setLocalisation(c.getLocalisation());
    e.setRegion(c.getRegion());
    e.setLatitude(c.getCoordonnees().latitude());
    e.setLongitude(c.getCoordonnees().longitude());
    e.setStatut(c.getStatut());
    e.setBudget(c.getBudget());
    e.setMontantMarche(c.getMontantMarche());
    e.setDateDebut(c.getDateDebut());
    e.setDateFinPrevue(c.getDateFinPrevue());
    e.setChefChantierId(c.getChefChantierId());
    e.setChefChantierNom(c.getChefChantierNom());
    e.setVersion(c.getVersion());
    return e;
  }

  /**
   * @param e entité
   * @return le chantier
   */
  public static Chantier versDomaine(ChantierEntity e) {
    return Chantier.reconstituer(
        new Chantier.Snapshot(
            e.getId(),
            new DonneesChantier(
                e.getCode(),
                e.getNom(),
                e.getLocalisation(),
                e.getRegion(),
                new Coordonnees(e.getLatitude(), e.getLongitude()),
                e.getBudget(),
                e.getMontantMarche(),
                e.getDateDebut(),
                e.getDateFinPrevue(),
                e.getChefChantierId(),
                e.getChefChantierNom()),
            e.getStatut(),
            e.getVersion()));
  }

  /**
   * @param m matériel
   * @return l'entité
   */
  public static MaterielEntity versEntite(Materiel m) {
    Materiel.Snapshot s = m.snapshot();
    DescriptionMateriel d = s.description();
    MaterielEntity e = new MaterielEntity();
    e.setId(s.id());
    e.setCode(d.code());
    e.setImmatriculation(d.immatriculation());
    e.setDesignation(d.designation());
    e.setCategorie(d.categorie());
    e.setTypeEnginId(d.typeEnginId());
    e.setMarque(d.marque());
    e.setModele(d.modele());
    e.setTarifJournalier(d.tarifJournalier());
    e.setValeurAcquisition(d.valeurAcquisition());
    e.setDateAcquisition(d.dateAcquisition());
    e.setEtat(s.etat());
    e.setHeuresFonctionnement(s.heuresFonctionnement());
    e.setHeuresDerniereMaintenance(s.heuresDerniereMaintenance());
    e.setDateReferenceMaintenance(s.dateReferenceMaintenance());
    e.setSeuilMaintenanceHeures(s.seuilMaintenanceHeures());
    e.setSeuilMaintenanceJours(s.seuilMaintenanceJours());
    e.setProchaineMaintenance(m.prochaineMaintenance());
    e.setDateDerniereAlerteMaintenance(s.dateDerniereAlerteMaintenance());
    e.setChantierActuelId(s.chantierActuelId());
    e.setLatitude(s.position() == null ? null : s.position().latitude());
    e.setLongitude(s.position() == null ? null : s.position().longitude());
    e.setFournisseurId(s.fournisseurId());
    e.setContratId(s.contratId());
    e.setReformeDemandee(s.reformeDemandee());
    e.setMotifReforme(s.motifReforme());
    e.setVersion(s.version());
    return e;
  }

  /**
   * @param e entité
   * @return le matériel
   */
  public static Materiel versDomaine(MaterielEntity e) {
    return Materiel.reconstituer(
        new Materiel.Snapshot(
            e.getId(),
            new DescriptionMateriel(
                e.getCode(),
                e.getImmatriculation(),
                e.getDesignation(),
                e.getCategorie(),
                e.getTypeEnginId(),
                e.getMarque(),
                e.getModele(),
                e.getTarifJournalier(),
                e.getValeurAcquisition(),
                e.getDateAcquisition()),
            e.getEtat(),
            e.getHeuresFonctionnement(),
            e.getHeuresDerniereMaintenance(),
            e.getDateReferenceMaintenance(),
            e.getSeuilMaintenanceHeures(),
            e.getSeuilMaintenanceJours(),
            e.getDateDerniereAlerteMaintenance(),
            e.getChantierActuelId(),
            Coordonnees.depuis(e.getLatitude(), e.getLongitude()),
            e.getFournisseurId(),
            e.getContratId(),
            e.isReformeDemandee(),
            e.getMotifReforme(),
            e.getVersion()));
  }

  /**
   * @param a affectation
   * @return l'entité
   */
  public static AffectationEntity versEntite(Affectation a) {
    Affectation.Snapshot s = a.snapshot();
    AffectationEntity e = new AffectationEntity();
    e.setId(s.id());
    e.setMaterielId(s.materielId());
    e.setChantierId(s.chantierId());
    e.setBesoinId(s.besoinId());
    e.setDateDebut(s.periodePrevue().debut());
    e.setDateFinPrevue(s.periodePrevue().fin());
    e.setDateFinReelle(s.dateFinReelle());
    e.setStatut(s.statut());
    e.setDateReception(s.dateReception());
    e.setObservationsReception(s.observationsReception());
    e.setCommentaireLiberation(s.commentaireLiberation());
    e.setFournisseurId(s.fournisseurId());
    e.setContratId(s.contratId());
    e.setAlerteFinEnvoyee(s.alerteFinEnvoyee());
    e.setVersion(s.version());
    return e;
  }

  /**
   * @param e entité
   * @return l'affectation
   */
  public static Affectation versDomaine(AffectationEntity e) {
    return Affectation.reconstituer(
        new Affectation.Snapshot(
            e.getId(),
            e.getMaterielId(),
            e.getChantierId(),
            e.getBesoinId(),
            new Periode(e.getDateDebut(), e.getDateFinPrevue()),
            e.getDateFinReelle(),
            e.getStatut(),
            e.getDateReception(),
            e.getObservationsReception(),
            e.getCommentaireLiberation(),
            e.getFournisseurId(),
            e.getContratId(),
            e.isAlerteFinEnvoyee(),
            e.getVersion()));
  }

  /**
   * @param m ordre de travail
   * @return l'entité
   */
  public static MaintenanceEntity versEntite(Maintenance m) {
    Maintenance.Snapshot s = m.snapshot();
    MaintenanceEntity e = new MaintenanceEntity();
    e.setId(s.id());
    e.setNumeroOt(s.numeroOt());
    e.setMaterielId(s.materielId());
    e.setChantierId(s.chantierId());
    e.setType(s.type());
    e.setStatut(s.statut());
    e.setDescription(s.description());
    e.setDatePlanifiee(s.datePlanifiee());
    e.setDateDebut(s.dateDebut());
    e.setDateFin(s.dateFin());
    e.setTechnicien(s.technicien());
    e.setCout(s.cout());
    e.setHeuresCompteur(s.heuresCompteur());
    e.setObservations(s.observations());
    e.setPanneId(s.panneId());
    e.setVersion(s.version());
    return e;
  }

  /**
   * @param e entité
   * @return l'ordre de travail
   */
  public static Maintenance versDomaine(MaintenanceEntity e) {
    return Maintenance.reconstituer(
        new Maintenance.Snapshot(
            e.getId(),
            e.getNumeroOt(),
            e.getMaterielId(),
            e.getChantierId(),
            e.getType(),
            e.getStatut(),
            e.getDescription(),
            e.getDatePlanifiee(),
            e.getDateDebut(),
            e.getDateFin(),
            e.getTechnicien(),
            e.getCout(),
            e.getHeuresCompteur(),
            e.getObservations(),
            e.getPanneId(),
            e.getVersion()));
  }

  /**
   * @param p panne
   * @return l'entité
   */
  public static PanneEntity versEntite(Panne p) {
    Panne.Snapshot s = p.snapshot();
    PanneEntity e = new PanneEntity();
    e.setId(s.id());
    e.setMaterielId(s.materielId());
    e.setChantierId(s.chantierId());
    e.setGravite(s.gravite());
    e.setDescription(s.description());
    e.setStatut(s.statut());
    e.setSignaleParId(s.signaleParId());
    e.setSignaleParNom(s.signaleParNom());
    e.setDateSignalement(s.dateSignalement());
    e.setMaintenanceId(s.maintenanceId());
    e.setVersion(s.version());
    return e;
  }

  /**
   * @param e entité
   * @return la panne
   */
  public static Panne versDomaine(PanneEntity e) {
    return Panne.reconstituer(
        new Panne.Snapshot(
            e.getId(),
            e.getMaterielId(),
            e.getChantierId(),
            e.getGravite(),
            e.getDescription(),
            e.getStatut(),
            e.getSignaleParId(),
            e.getSignaleParNom(),
            e.getDateSignalement(),
            e.getMaintenanceId(),
            e.getVersion()));
  }

  /**
   * @param m mouvement
   * @return l'entité
   */
  public static MouvementEntity versEntite(Mouvement m) {
    MouvementEntity e = new MouvementEntity();
    e.setId(m.id());
    e.setMaterielId(m.materielId());
    e.setType(m.type());
    e.setChantierOrigineId(m.chantierOrigineId());
    e.setChantierOrigineNom(m.chantierOrigineNom());
    e.setChantierDestinationId(m.chantierDestinationId());
    e.setChantierDestinationNom(m.chantierDestinationNom());
    e.setDate(m.date());
    e.setAuteurId(m.auteurId());
    e.setAuteurNom(m.auteurNom());
    e.setCommentaire(m.commentaire());
    return e;
  }

  /**
   * @param e entité
   * @return le mouvement
   */
  public static Mouvement versDomaine(MouvementEntity e) {
    return new Mouvement(
        e.getId(),
        e.getMaterielId(),
        e.getType(),
        e.getChantierOrigineId(),
        e.getChantierOrigineNom(),
        e.getChantierDestinationId(),
        e.getChantierDestinationNom(),
        e.getDate(),
        e.getAuteurId(),
        e.getAuteurNom(),
        e.getCommentaire());
  }
}
